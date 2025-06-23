package com.zanar.playera.dto;

import lombok.Data;

@Data
public class VenueResponseDTO {
  private Long venueId;
  private String name;
  private String location;
  private String description;
  private String contactNo;
  private Long ownerId;
  private String ownerName;
}