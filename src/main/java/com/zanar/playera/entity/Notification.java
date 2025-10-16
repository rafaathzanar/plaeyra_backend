package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@EqualsAndHashCode(exclude = { "user" })
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 1000)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationStatus status = NotificationStatus.UNREAD;

  @Column(name = "related_entity_type")
  private String relatedEntityType; // "BOOKING", "VENUE", etc.

  @Column(name = "related_entity_id")
  private Long relatedEntityId;

  @Column(name = "scheduled_for")
  private LocalDateTime scheduledFor; // For scheduled notifications

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  public enum NotificationType {
    BOOKING_CONFIRMED,
    BOOKING_REMINDER,
    BOOKING_CANCELLED,
    VENUE_UPDATE,
    SYSTEM_ANNOUNCEMENT
  }

  public enum NotificationStatus {
    UNREAD,
    READ,
    DELETED
  }
}
