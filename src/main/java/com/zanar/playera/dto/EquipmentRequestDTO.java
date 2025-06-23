package com.zanar.playera.dto;

import lombok.Data;

@Data
public class EquipmentRequestDTO {
  private String name;
  private double ratePerHour;
  private int availableQuantity;
  private Long courtId;
}