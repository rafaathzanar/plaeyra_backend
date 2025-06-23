package com.zanar.playera.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
  private double amount;
  private Long bookingId;
}