package com.zanar.playera.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierBenefitDTO {
  private String benefit;
  private String description;
  private boolean isActive;
}
