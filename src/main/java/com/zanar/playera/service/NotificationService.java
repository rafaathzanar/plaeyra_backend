package com.zanar.playera.service;

import com.zanar.playera.dto.NotificationDTO;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.Notification;
import com.zanar.playera.entity.User;
import com.zanar.playera.repo.NotificationRepository;
import com.zanar.playera.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  public void createBookingConfirmationNotification(Booking booking) {
    User customer = booking.getCustomer();
    if (customer == null)
      return;

    // Get venue name from first booking court
    String venueName = "Unknown Venue";
    if (booking.getBookingCourts() != null && !booking.getBookingCourts().isEmpty()) {
      venueName = booking.getBookingCourts().get(0).getCourt().getVenue().getName();
    }

    Notification notification = new Notification();
    notification.setUser(customer);
    notification.setTitle("Booking Confirmed");
    notification.setMessage(String.format("Your booking at %s has been confirmed for %s",
        venueName,
        booking.getBookingDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
    notification.setType(Notification.NotificationType.BOOKING_CONFIRMED);
    notification.setRelatedEntityType("BOOKING");
    notification.setRelatedEntityId(booking.getBookingId());

    notificationRepository.save(notification);
  }

  public void createBookingReminderNotification(Booking booking) {
    User customer = booking.getCustomer();
    if (customer == null)
      return;

    // Get venue name from first booking court
    String venueName = "Unknown Venue";
    if (booking.getBookingCourts() != null && !booking.getBookingCourts().isEmpty()) {
      venueName = booking.getBookingCourts().get(0).getCourt().getVenue().getName();
    }

    Notification notification = new Notification();
    notification.setUser(customer);
    notification.setTitle("Booking Reminder");
    notification.setMessage(String.format("You have a booking today at %s. Don't forget!",
        venueName));
    notification.setType(Notification.NotificationType.BOOKING_REMINDER);
    notification.setRelatedEntityType("BOOKING");
    notification.setRelatedEntityId(booking.getBookingId());

    notificationRepository.save(notification);
  }

  public List<NotificationDTO> getUserNotifications(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    List<Notification> notifications = notificationRepository.findActiveNotificationsByUser(user);

    return notifications.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  public long getUnreadNotificationCount(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    return notificationRepository.countUnreadNotificationsByUser(user);
  }

  public void markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("Notification not found"));

    if (!notification.getUser().getUserId().equals(userId)) {
      throw new RuntimeException("Unauthorized access to notification");
    }

    notification.setStatus(Notification.NotificationStatus.READ);
    notification.setReadAt(LocalDateTime.now());
    notificationRepository.save(notification);
  }

  public void deleteNotification(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("Notification not found"));

    if (!notification.getUser().getUserId().equals(userId)) {
      throw new RuntimeException("Unauthorized access to notification");
    }

    notification.setStatus(Notification.NotificationStatus.DELETED);
    notificationRepository.save(notification);
  }

  private NotificationDTO convertToDTO(Notification notification) {
    NotificationDTO dto = new NotificationDTO();
    dto.setNotificationId(notification.getNotificationId());
    dto.setTitle(notification.getTitle());
    dto.setMessage(notification.getMessage());
    dto.setType(notification.getType());
    dto.setStatus(notification.getStatus());
    dto.setRelatedEntityType(notification.getRelatedEntityType());
    dto.setRelatedEntityId(notification.getRelatedEntityId());
    dto.setScheduledFor(notification.getScheduledFor());
    dto.setCreatedAt(notification.getCreatedAt());
    dto.setReadAt(notification.getReadAt());

    // Calculate time ago
    LocalDateTime now = LocalDateTime.now();
    long minutesAgo = ChronoUnit.MINUTES.between(notification.getCreatedAt(), now);
    long hoursAgo = ChronoUnit.HOURS.between(notification.getCreatedAt(), now);
    long daysAgo = ChronoUnit.DAYS.between(notification.getCreatedAt(), now);

    if (minutesAgo < 60) {
      dto.setTimeAgo(minutesAgo + "m ago");
    } else if (hoursAgo < 24) {
      dto.setTimeAgo(hoursAgo + "h ago");
    } else {
      dto.setTimeAgo(daysAgo + "d ago");
    }

    dto.setToday(notification.getCreatedAt().toLocalDate().equals(LocalDate.now()));
    dto.setUnread(notification.getStatus() == Notification.NotificationStatus.UNREAD);

    return dto;
  }

  public void sendPaymentConfirmation(String customerEmail, String customerName, Double amount, String currency,
      String bookingId) {
    // This method is called by PaymentService but we don't need to implement it
    // since we're using in-app notifications instead of email notifications
    // The booking confirmation notification is already created in BookingService
  }

  public void sendPaymentFailure(String customerEmail, String customerName, String errorMessage, String bookingId) {
    // This method is called by PaymentService but we don't need to implement it
    // since we're using in-app notifications instead of email notifications
    // Payment failure notifications can be added here if needed
  }
}