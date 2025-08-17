package com.zanar.playera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyProgramDTO {
  private Long customerId;
  private int currentPoints;
  private String currentTier;
  private int pointsToNextTier;
  private double discountMultiplier;
  private LocalDateTime tierUpdatedAt;
  private List<LoyaltyTransactionDTO> recentTransactions;
  private List<RewardDTO> availableRewards;
  private List<RewardDTO> redeemedRewards;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoyaltyTransactionDTO {
    private Long transactionId;
    private String type; // EARN, REDEEM, EXPIRED
    private int points;
    private String description;
    private LocalDateTime transactionDate;
    private String bookingReference;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RewardDTO {
    private Long rewardId;
    private String name;
    private String description;
    private int pointsRequired;
    private String rewardType; // DISCOUNT, FREE_BOOKING, EQUIPMENT_RENTAL, etc.
    private double value;
    private boolean isActive;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime redeemedAt;
  }
}
