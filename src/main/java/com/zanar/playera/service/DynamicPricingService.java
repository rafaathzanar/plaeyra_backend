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
    try {
      Court court = courtRepository.findById(courtId)
          .orElseThrow(() -> new RuntimeException("Court not found"));

      log.info("Updating dynamic pricing for court {} with settings: {}", courtId, pricingSettings);

      // Update dynamic pricing fields with safe conversion
      if (pricingSettings.containsKey("dynamicPricingEnabled")) {
        Object value = pricingSettings.get("dynamicPricingEnabled");
        if (value instanceof Boolean) {
          court.setDynamicPricingEnabled((Boolean) value);
        } else if (value instanceof String) {
          court.setDynamicPricingEnabled(Boolean.parseBoolean((String) value));
        } else {
          court.setDynamicPricingEnabled(Boolean.TRUE.equals(value));
        }
        log.debug("Set dynamicPricingEnabled to: {}", court.getDynamicPricingEnabled());
      }

      if (pricingSettings.containsKey("peakHourStart")) {
        Object value = pricingSettings.get("peakHourStart");
        try {
          if (value instanceof String) {
            String timeStr = (String) value;
            // Handle different time formats
            if (timeStr.matches("\\d{1,2}:\\d{2}")) {
              court.setPeakHourStart(LocalTime.parse(timeStr));
            } else if (timeStr.matches("\\d{1,2}")) {
              court.setPeakHourStart(LocalTime.of(Integer.parseInt(timeStr), 0));
            } else {
              log.warn("Invalid peakHourStart format: {}, using default 18:00", timeStr);
              court.setPeakHourStart(LocalTime.of(18, 0));
            }
          }
        } catch (Exception e) {
          log.error("Error parsing peakHourStart: {}, using default 18:00", value, e);
          court.setPeakHourStart(LocalTime.of(18, 0));
        }
        log.debug("Set peakHourStart to: {}", court.getPeakHourStart());
      }

      if (pricingSettings.containsKey("peakHourEnd")) {
        Object value = pricingSettings.get("peakHourEnd");
        try {
          if (value instanceof String) {
            String timeStr = (String) value;
            // Handle different time formats
            if (timeStr.matches("\\d{1,2}:\\d{2}")) {
              court.setPeakHourEnd(LocalTime.parse(timeStr));
            } else if (timeStr.matches("\\d{1,2}")) {
              court.setPeakHourEnd(LocalTime.of(Integer.parseInt(timeStr), 0));
            } else {
              log.warn("Invalid peakHourEnd format: {}, using default 22:00", timeStr);
              court.setPeakHourEnd(LocalTime.of(22, 0));
            }
          }
        } catch (Exception e) {
          log.error("Error parsing peakHourEnd: {}, using default 22:00", value, e);
          court.setPeakHourEnd(LocalTime.of(22, 0));
        }
        log.debug("Set peakHourEnd to: {}", court.getPeakHourEnd());
      }

      if (pricingSettings.containsKey("peakHourMultiplier")) {
        Object value = pricingSettings.get("peakHourMultiplier");
        try {
          if (value instanceof Number) {
            court.setPeakHourMultiplier(((Number) value).doubleValue());
          } else if (value instanceof String) {
            court.setPeakHourMultiplier(Double.parseDouble((String) value));
          } else {
            log.warn("Invalid peakHourMultiplier type: {}, using default 1.5", value);
            court.setPeakHourMultiplier(1.5);
          }
        } catch (Exception e) {
          log.error("Error parsing peakHourMultiplier: {}, using default 1.5", value, e);
          court.setPeakHourMultiplier(1.5);
        }
        log.debug("Set peakHourMultiplier to: {}", court.getPeakHourMultiplier());
      }

      if (pricingSettings.containsKey("offPeakMultiplier")) {
        Object value = pricingSettings.get("offPeakMultiplier");
        try {
          if (value instanceof Number) {
            court.setOffPeakMultiplier(((Number) value).doubleValue());
          } else if (value instanceof String) {
            court.setOffPeakMultiplier(Double.parseDouble((String) value));
          } else {
            log.warn("Invalid offPeakMultiplier type: {}, using default 0.8", value);
            court.setOffPeakMultiplier(0.8);
          }
        } catch (Exception e) {
          log.error("Error parsing offPeakMultiplier: {}, using default 0.8", value, e);
          court.setOffPeakMultiplier(0.8);
        }
        log.debug("Set offPeakMultiplier to: {}", court.getOffPeakMultiplier());
      }

      if (pricingSettings.containsKey("weekendMultiplier")) {
        Object value = pricingSettings.get("weekendMultiplier");
        try {
          if (value instanceof Number) {
            court.setWeekendMultiplier(((Number) value).doubleValue());
          } else if (value instanceof String) {
            court.setWeekendMultiplier(Double.parseDouble((String) value));
          } else {
            log.warn("Invalid weekendMultiplier type: {}, using default 1.2", value);
            court.setWeekendMultiplier(1.2);
          }
        } catch (Exception e) {
          log.error("Error parsing weekendMultiplier: {}, using default 1.2", value, e);
          court.setWeekendMultiplier(1.2);
        }
        log.debug("Set weekendMultiplier to: {}", court.getWeekendMultiplier());
      }

      // Validate the settings
      validateDynamicPricingSettings(court);

      courtRepository.save(court);
      log.info("Successfully updated dynamic pricing settings for court {}", courtId);

    } catch (Exception e) {
      log.error("Error updating dynamic pricing for court {}: {}", courtId, e.getMessage(), e);
      throw new RuntimeException("Failed to update dynamic pricing settings: " + e.getMessage(), e);
    }
  }

  /**
   * Validate dynamic pricing settings
   */
  private void validateDynamicPricingSettings(Court court) {
    if (court.getDynamicPricingEnabled()) {
      // Ensure required fields are set
      if (court.getPeakHourStart() == null) {
        court.setPeakHourStart(LocalTime.of(18, 0));
      }
      if (court.getPeakHourEnd() == null) {
        court.setPeakHourEnd(LocalTime.of(22, 0));
      }
      if (court.getPeakHourMultiplier() == null || court.getPeakHourMultiplier() <= 0) {
        court.setPeakHourMultiplier(1.5);
      }
      if (court.getOffPeakMultiplier() == null || court.getOffPeakMultiplier() <= 0) {
        court.setOffPeakMultiplier(0.8);
      }
      if (court.getWeekendMultiplier() == null || court.getWeekendMultiplier() <= 0) {
        court.setWeekendMultiplier(1.2);
      }
    }
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

  /**
   * Get current dynamic pricing settings for a court
   */
  public Map<String, Object> getDynamicPricingSettings(Long courtId) {
    try {
      Court court = courtRepository.findById(courtId)
          .orElseThrow(() -> new RuntimeException("Court not found"));

      Map<String, Object> settings = new HashMap<>();
      settings.put("dynamicPricingEnabled",
          court.getDynamicPricingEnabled() != null ? court.getDynamicPricingEnabled() : false);
      settings.put("peakHourStart", court.getPeakHourStart() != null ? court.getPeakHourStart().toString() : "18:00");
      settings.put("peakHourEnd", court.getPeakHourEnd() != null ? court.getPeakHourEnd().toString() : "22:00");
      settings.put("peakHourMultiplier", court.getPeakHourMultiplier() != null ? court.getPeakHourMultiplier() : 1.5);
      settings.put("offPeakMultiplier", court.getOffPeakMultiplier() != null ? court.getOffPeakMultiplier() : 0.8);
      settings.put("weekendMultiplier", court.getWeekendMultiplier() != null ? court.getWeekendMultiplier() : 1.2);
      settings.put("basePrice", court.getPricePerHour());

      log.debug("Retrieved dynamic pricing settings for court {}: {}", courtId, settings);
      return settings;

    } catch (Exception e) {
      log.error("Error getting dynamic pricing settings for court {}: {}", courtId, e.getMessage(), e);
      throw new RuntimeException("Failed to get dynamic pricing settings: " + e.getMessage(), e);
    }
  }
}
