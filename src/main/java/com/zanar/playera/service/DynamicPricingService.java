package com.zanar.playera.service;

import com.zanar.playera.dto.DynamicPricingDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamicPricingService {

  @Autowired
  private VenueRepository venueRepository;

  @Autowired
  private CourtRepository courtRepository;

  @Value("${pricing.peak-hour-multiplier:1.5}")
  private double defaultPeakHourMultiplier;

  @Value("${pricing.off-peak-multiplier:0.8}")
  private double defaultOffPeakMultiplier;

  @Value("${pricing.weekend-multiplier:1.2}")
  private double defaultWeekendMultiplier;

  @Value("${pricing.holiday-multiplier:1.3}")
  private double defaultHolidayMultiplier;

  /**
   * Calculate dynamic price for a court at a specific time
   */
  public double calculateCourtPrice(Long courtId, LocalDateTime dateTime) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    if (!court.getDynamicPricingEnabled()) {
      return court.getPricePerHour().doubleValue();
    }

    return court.calculateDynamicPrice(dateTime.toLocalTime(), dateTime.getDayOfWeek()).doubleValue();
  }

  /**
   * Calculate dynamic price for a venue at a specific time
   */
  public double calculateVenuePrice(Long venueId, LocalDateTime dateTime) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    if (!venue.getDynamicPricingEnabled()) {
      return venue.getBasePrice();
    }

    return venue.calculateDynamicPrice(venue.getBasePrice(), dateTime.toLocalTime(), dateTime.getDayOfWeek());
  }

  /**
   * Get pricing for multiple time slots
   */
  public Map<LocalTime, Double> getPricingForTimeSlots(Long courtId, LocalDate date, List<LocalTime> timeSlots) {
    Map<LocalTime, Double> pricing = new HashMap<>();

    for (LocalTime time : timeSlots) {
      LocalDateTime dateTime = LocalDateTime.of(date, time);
      pricing.put(time, calculateCourtPrice(courtId, dateTime));
    }

    return pricing;
  }

  /**
   * Calculate demand-based pricing
   */
  public double calculateDemandBasedPricing(double basePrice, int availableSlots, int totalSlots) {
    if (totalSlots == 0)
      return basePrice;

    double occupancyRate = (double) (totalSlots - availableSlots) / totalSlots;

    if (occupancyRate > 0.8) {
      // High demand - increase price
      return basePrice * 1.3;
    } else if (occupancyRate > 0.6) {
      // Medium demand - slight increase
      return basePrice * 1.1;
    } else if (occupancyRate < 0.2) {
      // Low demand - decrease price
      return basePrice * 0.9;
    }

    return basePrice;
  }

  /**
   * Check if it's a holiday (Sri Lankan holidays)
   */
  public boolean isHoliday(LocalDate date) {
    // This is a simplified version. In production, you'd have a proper holiday
    // calendar
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    // Sri Lankan National Day
    if (month == 2 && day == 4)
      return true;

    // Sinhala and Tamil New Year (April 13-14)
    if (month == 4 && (day == 13 || day == 14))
      return true;

    // May Day
    if (month == 5 && day == 1)
      return true;

    // Independence Day
    if (month == 2 && day == 4)
      return true;

    // Christmas
    if (month == 12 && day == 25)
      return true;

    return false;
  }

  /**
   * Calculate holiday pricing
   */
  public double calculateHolidayPricing(double basePrice, LocalDate date) {
    if (isHoliday(date)) {
      return basePrice * defaultHolidayMultiplier;
    }
    return basePrice;
  }

  /**
   * Update dynamic pricing configuration for a court
   */
  public void updateCourtDynamicPricing(Long courtId, DynamicPricingDTO dto) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.setDynamicPricingEnabled(dto.isEnabled());
    court.setPeakHourMultiplier(dto.getPeakHourMultiplier());
    court.setOffPeakMultiplier(dto.getOffPeakMultiplier());
    court.setWeekendMultiplier(dto.getWeekendMultiplier());
    // setHolidayMultiplier method not available in Court entity
    court.setPeakHourStart(dto.getPeakHourStart());
    court.setPeakHourEnd(dto.getPeakHourEnd());

    courtRepository.save(court);
  }

  /**
   * Update dynamic pricing configuration for a venue
   */
  public void updateVenueDynamicPricing(Long venueId, DynamicPricingDTO dto) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.setDynamicPricingEnabled(dto.isEnabled());
    venue.setPeakHourMultiplier(dto.getPeakHourMultiplier());
    venue.setOffPeakMultiplier(dto.getOffPeakMultiplier());
    venue.setWeekendMultiplier(dto.getWeekendMultiplier());
    venue.setHolidayMultiplier(dto.getHolidayMultiplier());
    venue.setPeakHourStart(dto.getPeakHourStart());
    venue.setPeakHourEnd(dto.getPeakHourEnd());

    venueRepository.save(venue);
  }

  /**
   * Get pricing recommendations based on historical data
   */
  public Map<String, Double> getPricingRecommendations(Long courtId) {
    // This would integrate with analytics service to provide data-driven
    // recommendations
    Map<String, Double> recommendations = new HashMap<>();

    recommendations.put("peak_hour_multiplier", 1.4);
    recommendations.put("off_peak_multiplier", 0.85);
    recommendations.put("weekend_multiplier", 1.15);
    recommendations.put("holiday_multiplier", 1.25);

    return recommendations;
  }

  /**
   * Calculate bulk booking discount
   */
  public double calculateBulkBookingDiscount(double totalPrice, int hours) {
    if (hours >= 8) {
      return totalPrice * 0.15; // 15% discount for 8+ hours
    } else if (hours >= 4) {
      return totalPrice * 0.10; // 10% discount for 4+ hours
    } else if (hours >= 2) {
      return totalPrice * 0.05; // 5% discount for 2+ hours
    }
    return 0.0;
  }
}
