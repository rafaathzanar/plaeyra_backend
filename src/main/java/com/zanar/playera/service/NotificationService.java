package com.zanar.playera.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

  @Autowired
  private JavaMailSender emailSender;

  @Value("${firebase.project-id}")
  private String firebaseProjectId;

  @Value("${firebase.private-key}")
  private String firebasePrivateKey;

  @Value("${firebase.client-email}")
  private String firebaseClientEmail;

  @Value("${spring.mail.username}")
  private String fromEmail;

  private FirebaseMessaging firebaseMessaging;

  public NotificationService() {
    // Firebase will be initialized lazily when first needed
  }

  /**
   * Initialize Firebase for push notifications
   */
  private void initializeFirebase() {
    try {
      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseOptions options = FirebaseOptions.builder()
            .setProjectId(firebaseProjectId)
            .setCredentials(GoogleCredentials.fromStream(
                new ByteArrayInputStream(firebasePrivateKey.getBytes())))
            .build();

        FirebaseApp.initializeApp(options);
        this.firebaseMessaging = FirebaseMessaging.getInstance();
      }
    } catch (IOException e) {
      System.err.println("Failed to initialize Firebase: " + e.getMessage());
    }
  }

  /**
   * Get Firebase messaging instance, initializing if necessary
   */
  private FirebaseMessaging getFirebaseMessaging() {
    if (firebaseMessaging == null) {
      initializeFirebase();
    }
    return firebaseMessaging;
  }

  /**
   * Send push notification to a specific device
   */
  public CompletableFuture<String> sendPushNotification(String deviceToken, String title, String body,
      Map<String, String> data) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Message message = Message.builder()
            .setToken(deviceToken)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(data)
            .build();

        return getFirebaseMessaging().send(message);
      } catch (Exception e) {
        System.err.println("Failed to send push notification: " + e.getMessage());
        return null;
      }
    });
  }

  /**
   * Send push notification to multiple devices
   */
  public CompletableFuture<Map<String, String>> sendPushNotificationToMultipleDevices(
      List<String> deviceTokens, String title, String body, Map<String, String> data) {

    return CompletableFuture.supplyAsync(() -> {
      Map<String, String> results = new HashMap<>();

      for (String token : deviceTokens) {
        try {
          String messageId = sendPushNotification(token, title, body, data).get();
          results.put(token, messageId != null ? "SUCCESS" : "FAILED");
        } catch (Exception e) {
          results.put(token, "FAILED: " + e.getMessage());
        }
      }

      return results;
    });
  }

  /**
   * Send email notification
   */
  public void sendEmail(String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);

    emailSender.send(message);
  }

  /**
   * Send booking confirmation notification
   */
  public void sendBookingConfirmation(String customerEmail, String customerName, String venueName,
      String courtName, LocalDateTime bookingDate, String bookingReference) {
    String subject = "Booking Confirmation - " + venueName;
    String body = String.format(
        "Dear %s,\n\n" +
            "Your booking has been confirmed!\n\n" +
            "Venue: %s\n" +
            "Court: %s\n" +
            "Date: %s\n" +
            "Reference: %s\n\n" +
            "Thank you for choosing PlayEra!\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, venueName, courtName,
        bookingDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        bookingReference);

    sendEmail(customerEmail, subject, body);

    // Send push notification if device token is available
    // This would require storing device tokens in the customer entity
  }

  /**
   * Send booking cancellation notification
   */
  public void sendBookingCancellation(String customerEmail, String customerName, String venueName,
      String courtName, LocalDateTime bookingDate, String bookingReference) {
    String subject = "Booking Cancellation - " + venueName;
    String body = String.format(
        "Dear %s,\n\n" +
            "Your booking has been cancelled.\n\n" +
            "Venue: %s\n" +
            "Court: %s\n" +
            "Date: %s\n" +
            "Reference: %s\n\n" +
            "If you have any questions, please contact our support team.\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, venueName, courtName,
        bookingDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        bookingReference);

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send payment confirmation notification
   */
  public void sendPaymentConfirmation(String customerEmail, String customerName, double amount,
      String currency, String bookingReference) {
    String subject = "Payment Confirmation - PlayEra";
    String body = String.format(
        "Dear %s,\n\n" +
            "Your payment has been processed successfully!\n\n" +
            "Amount: %s %s\n" +
            "Booking Reference: %s\n" +
            "Date: %s\n\n" +
            "Thank you for your payment!\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, currency, amount, bookingReference,
        LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send payment failure notification
   */
  public void sendPaymentFailure(String customerEmail, String customerName, String reason,
      String bookingReference) {
    String subject = "Payment Failed - PlayEra";
    String body = String.format(
        "Dear %s,\n\n" +
            "Your payment has failed.\n\n" +
            "Reason: %s\n" +
            "Booking Reference: %s\n\n" +
            "Please try again or contact our support team for assistance.\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, reason, bookingReference);

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send reminder notification
   */
  public void sendBookingReminder(String customerEmail, String customerName, String venueName,
      String courtName, LocalDateTime bookingDate, String bookingReference) {
    String subject = "Booking Reminder - " + venueName;
    String body = String.format(
        "Dear %s,\n\n" +
            "This is a friendly reminder about your upcoming booking.\n\n" +
            "Venue: %s\n" +
            "Court: %s\n" +
            "Date: %s\n" +
            "Reference: %s\n\n" +
            "We look forward to seeing you!\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, venueName, courtName,
        bookingDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
        bookingReference);

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send loyalty points notification
   */
  public void sendLoyaltyPointsNotification(String customerEmail, String customerName, int pointsEarned,
      int totalPoints, String tier) {
    String subject = "Loyalty Points Earned - PlayEra";
    String body = String.format(
        "Dear %s,\n\n" +
            "Congratulations! You've earned %d loyalty points!\n\n" +
            "Points Earned: %d\n" +
            "Total Points: %d\n" +
            "Current Tier: %s\n\n" +
            "Keep booking to earn more points and unlock exclusive benefits!\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, pointsEarned, pointsEarned, totalPoints, tier);

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send tier upgrade notification
   */
  public void sendTierUpgradeNotification(String customerEmail, String customerName, String newTier,
      List<String> benefits) {
    String subject = "Tier Upgrade - PlayEra";
    String body = String.format(
        "Dear %s,\n\n" +
            "Congratulations! You've been upgraded to %s tier!\n\n" +
            "New Benefits:\n%s\n\n" +
            "Enjoy your exclusive benefits!\n\n" +
            "Best regards,\nPlayEra Team",
        customerName, newTier, String.join("\n", benefits));

    sendEmail(customerEmail, subject, body);
  }

  /**
   * Send venue owner notification
   */
  public void sendVenueOwnerNotification(String ownerEmail, String ownerName, String notificationType,
      String message) {
    String subject = "Venue Owner Notification - " + notificationType;
    String body = String.format(
        "Dear %s,\n\n" +
            "%s\n\n" +
            "Best regards,\nPlayEra Team",
        ownerName, message);

    sendEmail(ownerEmail, subject, body);
  }
}
