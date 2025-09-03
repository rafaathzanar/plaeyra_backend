package com.zanar.playera.dto;

import com.zanar.playera.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
  private Long notificationId;
  private String title;
  private String message;
  private Notification.NotificationType type;
  private Notification.NotificationStatus status;
  private String relatedEntityType;
  private Long relatedEntityId;
  private LocalDateTime scheduledFor;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;

  // Additional fields for display
  private String timeAgo;
  private Boolean today;
  private Boolean unread;
}
