package com.zanar.playera.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long paymentId;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be positive")
  private Double amount;

  @NotNull(message = "Currency is required")
  private String currency = "LKR"; // Sri Lankan Rupees

  @Enumerated(EnumType.STRING)
  private PaymentStatus status = PaymentStatus.PENDING;

  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod = PaymentMethod.CARD;

  @NotNull(message = "Payment date is required")
  private LocalDateTime paymentDate;

  private String transactionId; // External transaction reference

  private LocalDateTime processedAt;

  private LocalDateTime refundedAt;

  // Stripe Integration Fields
  private String stripePaymentIntentId;

  private String stripeChargeId;

  private String stripeCustomerId;

  private String stripeRefundId;

  // Payment Details
  private String description;

  private String receiptUrl;

  private String failureReason;

  private String failureCode;

  // Refund Information
  private Double refundAmount;

  private String refundReason;

  private String refundNotes;

  // Customer Information
  private String customerEmail;

  private String customerName;

  private String customerPhone;

  // Booking Reference
  @OneToOne(mappedBy = "payment")
  private Booking booking;

  // Commission and Fees
  private Double platformFee;

  private Double venueOwnerAmount;

  private Double stripeFee;

  // Metadata
  private String metadata; // JSON string for additional data

  public enum PaymentStatus {
    PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
  }

  public enum PaymentMethod {
    CARD, BANK_TRANSFER, WALLET, LOYALTY_POINTS, CASH
  }

  // Helper methods
  public boolean isSuccessful() {
    return status == PaymentStatus.SUCCEEDED;
  }

  public boolean isFailed() {
    return status == PaymentStatus.FAILED;
  }

  public boolean isRefunded() {
    return status == PaymentStatus.REFUNDED || status == PaymentStatus.PARTIALLY_REFUNDED;
  }

  public boolean canRefund() {
    return isSuccessful() && !isRefunded();
  }

  public void markAsProcessed() {
    this.status = PaymentStatus.SUCCEEDED;
    this.processedAt = LocalDateTime.now();
  }

  public void markAsFailed(String reason, String code) {
    this.status = PaymentStatus.FAILED;
    this.failureReason = reason;
    this.failureCode = code;
  }

  public void markAsRefunded(Double refundAmount, String reason) {
    this.status = PaymentStatus.REFUNDED;
    this.refundAmount = refundAmount;
    this.refundReason = reason;
    this.refundedAt = LocalDateTime.now();
  }

  public void markAsPartiallyRefunded(Double refundAmount, String reason) {
    this.status = PaymentStatus.PARTIALLY_REFUNDED;
    this.refundAmount = refundAmount;
    this.refundReason = reason;
    this.refundedAt = LocalDateTime.now();
  }

  public Double getRemainingAmount() {
    if (refundAmount == null) {
      return amount;
    }
    return amount - refundAmount;
  }

  public boolean isFullRefund() {
    return refundAmount != null && refundAmount.equals(amount);
  }

  public void calculateFees(Double platformFeePercentage, Double stripeFeePercentage) {
    this.platformFee = amount * platformFeePercentage;
    this.stripeFee = amount * stripeFeePercentage;
    this.venueOwnerAmount = amount - this.platformFee - this.stripeFee;
  }
}