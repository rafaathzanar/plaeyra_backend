package com.zanar.playera.dto;

import lombok.Data;

@Data
public class EquipmentResponseDTO {
  private Long equipmentId;
  private String name;
  private double ratePerHour;
  private int availableQuantity;
  private Long courtId;
  private String courtName;
}