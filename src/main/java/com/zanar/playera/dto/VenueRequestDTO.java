package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
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
    private List<String> images;
    private List<String> amenities;
    private Long ownerId;
}