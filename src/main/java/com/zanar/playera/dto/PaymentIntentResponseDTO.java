package com.zanar.playera.dto;

import lombok.Data;

@Data
public class PaymentIntentResponseDTO {

  private String paymentIntentId;
  private String clientSecret;
  private String status;
  private Long amount;
  private String currency;
  private String description;
  private String customerEmail;
  private String publishableKey;

  public PaymentIntentResponseDTO() {
  }

  public PaymentIntentResponseDTO(String paymentIntentId, String clientSecret, String status,
      Long amount, String currency, String description,
      String customerEmail, String publishableKey) {
    this.paymentIntentId = paymentIntentId;
    this.clientSecret = clientSecret;
    this.status = status;
    this.amount = amount;
    this.currency = currency;
    this.description = description;
    this.customerEmail = customerEmail;
    this.publishableKey = publishableKey;
  }
}
