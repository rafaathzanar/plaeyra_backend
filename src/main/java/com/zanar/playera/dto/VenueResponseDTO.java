package com.zanar.playera.dto;

import lombok.Data;
import java.util.List;

@Data
public class VenueResponseDTO {
  private Long venueId;
  private String name;
  private String address;
  private String location;
  private String description;
  private String contactNo;
  private List<String> images;
  private List<String> amenities;
  private Long ownerId;
  private String ownerName;
}