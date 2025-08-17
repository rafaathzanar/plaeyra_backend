package com.zanar.playera.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long reviewId;

  @NotBlank(message = "Review comment is required")
  @Size(min = 10, max = 1000, message = "Review comment must be between 10 and 1000 characters")
  private String comment;

  @NotNull(message = "Rating is required")
  @Min(value = 1, message = "Rating must be at least 1")
  @Max(value = 5, message = "Rating must be at most 5")
  private Integer rating;

  @NotNull(message = "Review date is required")
  private LocalDateTime reviewDate;

  @Enumerated(EnumType.STRING)
  private ReviewStatus status = ReviewStatus.PENDING;

  @Enumerated(EnumType.STRING)
  private ReviewType reviewType = ReviewType.VENUE;

  // Moderation fields
  private boolean isModerated = false;

  private LocalDateTime moderatedAt;

  private String moderationNotes;

  private String moderatorId;

  // Response fields
  private String ownerResponse;

  private LocalDateTime responseDate;

  private String responderId;

  // Additional review details
  private String title; // Optional review title

  private boolean isAnonymous = false;

  private boolean isVerifiedBooking = false;

  private String bookingReference;

  private String helpfulVotes; // JSON string for tracking helpful votes

  private String reportReasons; // JSON string for tracking report reasons

  private boolean isReported = false;

  private LocalDateTime reportedAt;

  private String reportNotes;

  @ManyToOne
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @ManyToOne
  @JoinColumn(name = "court_id")
  private Court court;

  @ManyToOne
  @JoinColumn(name = "equipment_id")
  private Equipment equipment;

  public enum ReviewStatus {
    PENDING, APPROVED, REJECTED, HIDDEN, DELETED
  }

  public enum ReviewType {
    VENUE, COURT, EQUIPMENT, SERVICE, OVERALL
  }

  // Helper methods
  public boolean isApproved() {
    return status == ReviewStatus.APPROVED;
  }

  public boolean isRejected() {
    return status == ReviewStatus.REJECTED;
  }

  public boolean isHidden() {
    return status == ReviewStatus.HIDDEN;
  }

  public boolean hasOwnerResponse() {
    return ownerResponse != null && !ownerResponse.trim().isEmpty();
  }

  public boolean isModerated() {
    return isModerated;
  }

  public void approve(String moderatorId) {
    this.status = ReviewStatus.APPROVED;
    this.isModerated = true;
    this.moderatedAt = LocalDateTime.now();
    this.moderatorId = moderatorId;
  }

  public void reject(String moderatorId, String notes) {
    this.status = ReviewStatus.REJECTED;
    this.isModerated = true;
    this.moderatedAt = LocalDateTime.now();
    this.moderatorId = moderatorId;
    this.moderationNotes = notes;
  }

  public void hide(String moderatorId, String notes) {
    this.status = ReviewStatus.HIDDEN;
    this.isModerated = true;
    this.moderatedAt = LocalDateTime.now();
    this.moderatorId = moderatorId;
    this.moderationNotes = notes;
  }

  public void addOwnerResponse(String response, String responderId) {
    this.ownerResponse = response;
    this.responseDate = LocalDateTime.now();
    this.responderId = responderId;
  }

  public void report(String reason, String notes) {
    this.isReported = true;
    this.reportedAt = LocalDateTime.now();
    this.reportReasons = reason;
    this.reportNotes = notes;
  }

  @PrePersist
  protected void onCreate() {
    if (reviewDate == null) {
      reviewDate = LocalDateTime.now();
    }
  }
}