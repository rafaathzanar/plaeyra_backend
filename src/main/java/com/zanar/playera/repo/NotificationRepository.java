package com.zanar.playera.repo;

import com.zanar.playera.entity.Notification;
import com.zanar.playera.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserAndStatusOrderByCreatedAtDesc(User user, Notification.NotificationStatus status);

  List<Notification> findByUserOrderByCreatedAtDesc(User user);

  @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.status != 'DELETED' ORDER BY n.createdAt DESC")
  List<Notification> findActiveNotificationsByUser(@Param("user") User user);

  @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.status = 'UNREAD'")
  long countUnreadNotificationsByUser(@Param("user") User user);

  @Query("SELECT n FROM Notification n WHERE n.scheduledFor <= :now AND n.status = 'UNREAD'")
  List<Notification> findScheduledNotificationsForNow(@Param("now") LocalDateTime now);

  @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type AND n.relatedEntityId = :entityId AND n.status != 'DELETED'")
  List<Notification> findByUserAndTypeAndEntityId(@Param("user") User user,
      @Param("type") Notification.NotificationType type,
      @Param("entityId") Long entityId);
}
