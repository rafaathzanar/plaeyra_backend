package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;

@Data
public class PaymentIntentRequestDTO {

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private Long amount;

  @NotNull(message = "Currency is required")
  private String currency = "LKR";

  @NotNull(message = "Description is required")
  private String description;

  @Email(message = "Valid email is required")
  private String customerEmail;

  private String customerName;

  private String customerPhone;

  // Optional: Booking reference
  private Long bookingId;

  // Optional: Metadata
  private String metadata;
}
