package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

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
    @NotNull(message = "Venue ID is required")
    private Long venueId;
}