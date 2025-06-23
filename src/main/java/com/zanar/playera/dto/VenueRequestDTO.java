package com.zanar.playera.dto;

import lombok.Data;

@Data
public class VenueRequestDTO {
  private String name;
  private String location;
  private String description;
  private String contactNo;
  private Long ownerId;
}