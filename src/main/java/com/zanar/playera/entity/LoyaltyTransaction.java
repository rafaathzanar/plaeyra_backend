package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long transactionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  @Column(nullable = false)
  private Integer pointsChange;

  @Column(nullable = false)
  private Integer goldCoinsChange;

  @Column(nullable = false)
  private Integer pointsBalance;

  @Column(nullable = false)
  private Integer goldCoinsBalance;

  @Column(length = 500)
  private String description;

  @Column(length = 100)
  private String referenceId; // Booking ID, Review ID, etc.

  @Column(nullable = false)
  private LocalDateTime transactionDate;

  @PrePersist
  protected void onCreate() {
    if (transactionDate == null) {
      transactionDate = LocalDateTime.now();
    }
  }

  public enum TransactionType {
    BOOKING_EARNED("Booking Completed"),
    REVIEW_EARNED("Review Submitted"),
    REDEMPTION_USED("Points/Coins Redeemed"),
    TIER_UPGRADE("Tier Upgraded"),
    ADMIN_ADJUSTMENT("Admin Adjustment"),
    REFUND_REVERSED("Refund Reversal");

    private final String displayName;

    TransactionType(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
