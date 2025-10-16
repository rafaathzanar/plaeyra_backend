package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentRequestDTO {
  private double amount;
  private Long bookingId;
  private String paymentMethod;
  private String status;
  private String transactionId;
  private LocalDateTime paymentDate;
  private String description;
}