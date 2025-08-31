package com.zanar.playera.service;

import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Slot;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicPricingService {

  private final CourtRepository courtRepository;
  private final SlotRepository slotRepository;

  /**
   * Calculate dynamic price for a specific time slot
   */
  public BigDecimal calculateSlotPrice(Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
    if (!Boolean.TRUE.equals(court.getDynamicPricingEnabled())) {
      return court.getPricePerHour();
    }

    // Calculate duration in hours
    long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
    BigDecimal durationHours = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 2,
        RoundingMode.HALF_UP);

    // Get base price per hour
    BigDecimal basePrice = court.getPricePerHour();
    if (basePrice == null) {
      log.warn("Base price not set for court {}", court.getCourtId());
      return BigDecimal.ZERO;
    }

    // Calculate price for each hour segment
    BigDecimal totalPrice = BigDecimal.ZERO;
    LocalTime currentTime = startTime;

    while (currentTime.isBefore(endTime)) {
      LocalTime segmentEnd = currentTime.plusHours(1);
      if (segmentEnd.isAfter(endTime)) {
        segmentEnd = endTime;
      }

      // Calculate price for this hour segment
      BigDecimal segmentPrice = calculateHourPrice(court, date, currentTime);
      totalPrice = totalPrice.add(segmentPrice);

      currentTime = segmentEnd;
    }

    return totalPrice.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Calculate price for a specific hour
   */
  private BigDecimal calculateHourPrice(Court court, LocalDate date, LocalTime time) {
    BigDecimal basePrice = court.getPricePerHour();
    BigDecimal multiplier = BigDecimal.ONE;
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    // Peak hour pricing
    if (isPeakHour(court, time)) {
      multiplier = multiplier
          .multiply(BigDecimal.valueOf(court.getPeakHourMultiplier() != null ? court.getPeakHourMultiplier() : 1.5));
      log.debug("Peak hour multiplier applied: {}", court.getPeakHourMultiplier());
    } else {
      multiplier = multiplier
          .multiply(BigDecimal.valueOf(court.getOffPeakMultiplier() != null ? court.getOffPeakMultiplier() : 0.8));
      log.debug("Off-peak multiplier applied: {}", court.getOffPeakMultiplier());
    }

    // Weekend pricing
    if (isWeekend(dayOfWeek)) {
      multiplier = multiplier
          .multiply(BigDecimal.valueOf(court.getWeekendMultiplier() != null ? court.getWeekendMultiplier() : 1.2));
      log.debug("Weekend multiplier applied: {}", court.getWeekendMultiplier());
    }

    // Special day pricing (holidays, events, etc.)
    BigDecimal specialPrice = getSpecialDayPrice(court, date, time);
    if (specialPrice != null) {
      return specialPrice;
    }

    return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Check if time is during peak hours
   */
  private boolean isPeakHour(Court court, LocalTime time) {
    if (court.getPeakHourStart() == null || court.getPeakHourEnd() == null) {
      return false;
    }

    // Handle peak hours that span midnight
    if (court.getPeakHourStart().isAfter(court.getPeakHourEnd())) {
      return time.isAfter(court.getPeakHourStart()) || time.isBefore(court.getPeakHourEnd());
    } else {
      return time.isAfter(court.getPeakHourStart()) && time.isBefore(court.getPeakHourEnd());
    }
  }

  /**
   * Check if day is weekend
   */
  private boolean isWeekend(DayOfWeek dayOfWeek) {
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
  }

  /**
   * Get special day pricing (holidays, events, etc.)
   */
  private BigDecimal getSpecialDayPrice(Court court, LocalDate date, LocalTime time) {
    // Check if there's a special price for this day
    Court.CourtAvailability availability = court.getAvailabilitySchedule().get(date.getDayOfWeek());
    if (availability != null && availability.getSpecialPrice() != null && availability.getSpecialPrice() > 0) {
      return BigDecimal.valueOf(availability.getSpecialPrice());
    }

    // Check if it's a holiday
    if (isHoliday(date)) {
      return court.getPricePerHour().multiply(BigDecimal.valueOf(1.5)); // 50% premium for holidays
    }

    return null; // No special pricing
  }

  /**
   * Check if date is a holiday
   */
  private boolean isHoliday(LocalDate date) {
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    // Example holidays (Sri Lanka) - you can enhance this with a holiday API or
    // database
    return (month == 1 && day == 1) || // New Year
        (month == 4 && day == 13) || // Sinhala & Tamil New Year
        (month == 5 && day == 1) || // May Day
        (month == 12 && day == 25); // Christmas
  }

  /**
   * Get pricing summary for a date range
   */
  public Map<LocalDate, Map<LocalTime, BigDecimal>> getPricingSummary(Long courtId, LocalDate startDate,
      LocalDate endDate) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<LocalDate, Map<LocalTime, BigDecimal>> pricingSummary = new HashMap<>();
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      if (isCourtAvailableOnDate(court, currentDate)) {
        Map<LocalTime, BigDecimal> dayPricing = new HashMap<>();

        // Generate hourly pricing for the day
        LocalTime currentTime = court.getOpeningTime();
        while (currentTime.isBefore(court.getClosingTime())) {
          BigDecimal price = calculateHourPrice(court, currentDate, currentTime);
          dayPricing.put(currentTime, price);
          currentTime = currentTime.plusHours(1);
        }

        pricingSummary.put(currentDate, dayPricing);
      }

      currentDate = currentDate.plusDays(1);
    }

    return pricingSummary;
  }

  /**
   * Check if court is available on a specific date
   */
  private boolean isCourtAvailableOnDate(Court court, LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    // Check if court is active on weekends
    if (isWeekend(dayOfWeek) && !Boolean.TRUE.equals(court.getIsActiveOnWeekends())) {
      return false;
    }

    // Check if court is active on holidays
    if (isHoliday(date) && !Boolean.TRUE.equals(court.getIsActiveOnHolidays())) {
      return false;
    }

    // Check if court status is active
    return Court.CourtStatus.ACTIVE.equals(court.getStatus());
  }

  /**
   * Update dynamic pricing settings for a court
   */
  public void updateDynamicPricing(Long courtId, Map<String, Object> pricingSettings) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // Update dynamic pricing fields
    if (pricingSettings.containsKey("dynamicPricingEnabled")) {
      court.setDynamicPricingEnabled((Boolean) pricingSettings.get("dynamicPricingEnabled"));
    }

    if (pricingSettings.containsKey("peakHourStart")) {
      court.setPeakHourStart(LocalTime.parse((String) pricingSettings.get("peakHourStart")));
    }

    if (pricingSettings.containsKey("peakHourEnd")) {
      court.setPeakHourEnd(LocalTime.parse((String) pricingSettings.get("peakHourEnd")));
    }

    if (pricingSettings.containsKey("peakHourMultiplier")) {
      court.setPeakHourMultiplier(Double.parseDouble(pricingSettings.get("peakHourMultiplier").toString()));
    }

    if (pricingSettings.containsKey("offPeakMultiplier")) {
      court.setOffPeakMultiplier(Double.parseDouble(pricingSettings.get("offPeakMultiplier").toString()));
    }

    if (pricingSettings.containsKey("weekendMultiplier")) {
      court.setWeekendMultiplier(Double.parseDouble(pricingSettings.get("weekendMultiplier").toString()));
    }

    courtRepository.save(court);
    log.info("Updated dynamic pricing settings for court {}", courtId);
  }

  /**
   * Get pricing analytics for a court
   */
  public Map<String, Object> getPricingAnalytics(Long courtId, LocalDate startDate, LocalDate endDate) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> analytics = new HashMap<>();

    // Get pricing summary
    Map<LocalDate, Map<LocalTime, BigDecimal>> pricingSummary = getPricingSummary(courtId, startDate, endDate);

    // Calculate average prices
    BigDecimal totalPrice = BigDecimal.ZERO;
    int totalSlots = 0;
    BigDecimal maxPrice = BigDecimal.ZERO;
    BigDecimal minPrice = court.getPricePerHour();

    for (Map<LocalTime, BigDecimal> dayPricing : pricingSummary.values()) {
      for (BigDecimal price : dayPricing.values()) {
        totalPrice = totalPrice.add(price);
        totalSlots++;

        if (price.compareTo(maxPrice) > 0) {
          maxPrice = price;
        }

        if (price.compareTo(minPrice) < 0) {
          minPrice = price;
        }
      }
    }

    BigDecimal averagePrice = totalSlots > 0
        ? totalPrice.divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    analytics.put("averagePrice", averagePrice);
    analytics.put("maxPrice", maxPrice);
    analytics.put("minPrice", minPrice);
    analytics.put("totalSlots", totalSlots);
    analytics.put("pricingSummary", pricingSummary);

    return analytics;
  }
}
