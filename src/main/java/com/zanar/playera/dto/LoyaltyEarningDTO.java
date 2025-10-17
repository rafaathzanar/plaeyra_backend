package com.zanar.playera.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyEarningDTO {
  private Long customerId;
  private Integer pointsEarned;
  private Integer goldCoinsEarned;
  private String earningType;
  private String description;
  private String referenceId;
}
