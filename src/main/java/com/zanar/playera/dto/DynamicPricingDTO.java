package com.zanar.playera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicPricingDTO {
  private Long venueId;
  private Long courtId;
  private boolean enabled;
  private double basePrice;
  private double peakHourMultiplier;
  private double offPeakMultiplier;
  private double weekendMultiplier;
  private double holidayMultiplier;
  private LocalTime peakHourStart;
  private LocalTime peakHourEnd;
  private Map<DayOfWeek, SpecialDayPricingDTO> specialDayPricing;
  private List<SpecialEventPricingDTO> specialEvents;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SpecialDayPricingDTO {
    private DayOfWeek day;
    private double multiplier;
    private double fixedPrice;
    private String notes;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SpecialEventPricingDTO {
    private String eventName;
    private String eventDate;
    private double multiplier;
    private double fixedPrice;
    private String description;
  }
}
