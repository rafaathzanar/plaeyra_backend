package com.zanar.playera.dto;

import lombok.Data;

@Data
public class CourtRequestDTO {
  private String courtName;
  private String type;
  private int capacity;
  private double pricePerHour;
  private Long venueId;
}