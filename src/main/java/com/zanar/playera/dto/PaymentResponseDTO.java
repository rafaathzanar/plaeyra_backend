package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDTO {
  private Long paymentId;
  private double amount;
  private String status;
  private LocalDateTime paymentDate;
  private Long bookingId;
}