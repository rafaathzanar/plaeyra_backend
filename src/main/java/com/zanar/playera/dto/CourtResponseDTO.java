package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class CourtResponseDTO {
  // Backward compatibility fields (for existing frontend)
  private Long courtId;
  private String name; // Alias for courtName
  private String sportType; // Alias for type
  private String surfaceType; // Legacy field
  private String status;
  private Double pricePerHour;
  private String description;
  private String imageUrl; // Legacy field

  // New comprehensive fields
  private String courtName;
  private String type;
  private Integer capacity;
  private Boolean isIndoor;
  private Boolean isLighted;
  private Boolean isAirConditioned;
  private Integer minBookingDuration;
  private Integer maxBookingDuration;

  // Time slot management fields
  private LocalTime openingTime;
  private LocalTime closingTime;
  private Integer slotDurationMinutes;
  private Boolean isActiveOnWeekends;
  private Boolean isActiveOnHolidays;

  // Break times
  private Boolean hasBreakTime;
  private LocalTime breakStartTime;
  private LocalTime breakEndTime;

  // Dynamic pricing fields
  private Boolean dynamicPricingEnabled;
  private LocalTime peakHourStart;
  private LocalTime peakHourEnd;
  private Double peakHourMultiplier;
  private Double offPeakMultiplier;
  private Double weekendMultiplier;

  // Maintenance fields
  private Boolean maintenanceMode;
  private LocalTime maintenanceStartTime;
  private LocalTime maintenanceEndTime;

  // Venue information
  private Long venueId;
  private String venueName;
}