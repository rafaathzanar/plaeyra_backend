package com.zanar.playera.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyInfoDTO {
  private Long customerId;
  private String customerName;
  private String customerEmail;
  private Integer currentPoints;
  private Integer goldCoins;
  private String currentTier;
  private Integer pointsToNextTier;
  private String nextTier;
  private Double discountMultiplier;
  private Double goldCoinMultiplier;
  private LocalDateTime tierUpdatedAt;
  private Integer totalBookings;
  private Double totalSpent;
  private LocalDateTime lastBookingAt;
  private List<LoyaltyTransactionDTO> recentTransactions;
  private List<TierBenefitDTO> currentBenefits;
}
