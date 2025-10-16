package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class VenueRequestDTO {
    @NotBlank(message = "Venue name is required")
    private String name;

    @NotBlank(message = "Venue address is required")
    private String address;

    private String location;
    private String description;
    private String contactNo;
    private String email;
    private String website;
    private String latitude;
    private String longitude;
    private String venueType;
    private Integer maxCapacity;
    private String status;

    // Amenities
    private Boolean parkingAvailable;
    private Boolean foodAvailable;
    private Boolean changingRoomsAvailable;
    private Boolean showerAvailable;
    private Boolean wifiAvailable;

    // Business details
    private String openingHours;
    private BigDecimal basePrice;
    private String cancellationPolicy;
    private String refundPolicy;

    // Lists
    private List<String> images;
    private List<String> amenities;
    private List<String> sportsTypes;

    // Owner association
    @NotNull(message = "Owner ID is required")
    private Long ownerId;
}