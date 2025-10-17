package com.zanar.playera.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransactionDTO {
  private Long transactionId;
  private String transactionType;
  private String displayName;
  private Integer pointsChange;
  private Integer goldCoinsChange;
  private Integer pointsBalance;
  private Integer goldCoinsBalance;
  private String description;
  private String referenceId;
  private LocalDateTime transactionDate;
}
