package com.zanar.playera.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldCoinRedemptionDTO {
  private Long customerId;
  private Integer coinsToRedeem;
  private Double discountAmount;
  private String description;
  private String bookingReference;
}
