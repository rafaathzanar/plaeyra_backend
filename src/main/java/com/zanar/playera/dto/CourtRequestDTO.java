package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;
import java.util.List;

@Data
public class CourtRequestDTO {
    @NotBlank(message = "Court name is required")
    private String courtName;

    @NotBlank(message = "Court type is required")
    private String type;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    @Min(value = 0, message = "Price per hour must be non-negative")
    private double pricePerHour;

    // Optional fields for updates
    private String description;
    private Boolean isIndoor;
    private Boolean isLighted;
    private Boolean isAirConditioned;
    private Integer minBookingDuration;
    private Integer maxBookingDuration;

    // Time slot management
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Integer slotDurationMinutes;
    private Boolean isActiveOnWeekends;
    private Boolean isActiveOnHolidays;

    // Break times
    private Boolean hasBreakTime;
    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    // Dynamic pricing
    private Boolean dynamicPricingEnabled;
    private LocalTime peakHourStart;
    private LocalTime peakHourEnd;
    private Double peakHourMultiplier;
    private Double offPeakMultiplier;
    private Double weekendMultiplier;

    // Maintenance
    private Boolean maintenanceMode;
    private LocalTime maintenanceStartTime;
    private LocalTime maintenanceEndTime;

    // Venue ID - optional for updates
    private Long venueId;

    // Images
    private List<String> images;
}