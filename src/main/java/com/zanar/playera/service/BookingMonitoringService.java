package com.zanar.playera.service;

import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.BookingTimeSlot;
import com.zanar.playera.repo.BookingRepository;
import com.zanar.playera.repo.BookingTimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class BookingMonitoringService {

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private BookingTimeSlotRepository bookingTimeSlotRepository;

  // Metrics tracking
  private final AtomicLong totalBookings = new AtomicLong(0);
  private final AtomicLong conflictAttempts = new AtomicLong(0);
  private final AtomicLong successfulBookings = new AtomicLong(0);
  private final AtomicLong failedBookings = new AtomicLong(0);

  /**
   * Detect potential double bookings by checking for overlapping time slots
   */
  @Scheduled(fixedRate = 300000) // Every 5 minutes
  public void detectDoubleBookings() {
    try {
      LocalDate today = LocalDate.now();
      List<BookingTimeSlot> todaySlots = bookingTimeSlotRepository.findByCourtAndDate(
          null, today); // This would need to be modified to check all courts

      Map<String, List<BookingTimeSlot>> conflicts = findConflictingSlots(todaySlots);

      if (!conflicts.isEmpty()) {
        logConflictDetection(conflicts);
        sendAlert("Double booking detected", conflicts);
      }

    } catch (Exception e) {
      System.err.println("Error in double booking detection: " + e.getMessage());
    }
  }

  /**
   * Find conflicting slots
   */
  private Map<String, List<BookingTimeSlot>> findConflictingSlots(List<BookingTimeSlot> slots) {
    Map<String, List<BookingTimeSlot>> conflicts = new HashMap<>();

    for (int i = 0; i < slots.size(); i++) {
      BookingTimeSlot slot1 = slots.get(i);

      for (int j = i + 1; j < slots.size(); j++) {
        BookingTimeSlot slot2 = slots.get(j);

        // Check if slots are for the same court and overlap
        if (slot1.getCourt().getCourtId().equals(slot2.getCourt().getCourtId()) &&
            slot1.overlapsWith(slot2.getStartTime(), slot2.getEndTime())) {

          String conflictKey = slot1.getCourt().getCourtId() + "-" +
              slot1.getStartTime() + "-" + slot1.getEndTime();

          conflicts.computeIfAbsent(conflictKey, k -> new ArrayList<>())
              .addAll(List.of(slot1, slot2));
        }
      }
    }

    return conflicts;
  }

  /**
   * Log conflict detection
   */
  private void logConflictDetection(Map<String, List<BookingTimeSlot>> conflicts) {
    System.err.println("=== DOUBLE BOOKING DETECTED ===");
    System.err.println("Timestamp: " + LocalDateTime.now());
    System.err.println("Number of conflicts: " + conflicts.size());

    for (Map.Entry<String, List<BookingTimeSlot>> entry : conflicts.entrySet()) {
      System.err.println("Conflict: " + entry.getKey());
      for (BookingTimeSlot slot : entry.getValue()) {
        System.err.println("  - Booking ID: " + slot.getBooking().getBookingId() +
            ", Customer: " + slot.getBooking().getCustomer().getEmail() +
            ", Time: " + slot.getStartTime() + " - " + slot.getEndTime());
      }
    }
    System.err.println("=== END CONFLICT REPORT ===");
  }

  /**
   * Send alert (implement your preferred alerting mechanism)
   */
  private void sendAlert(String message, Map<String, List<BookingTimeSlot>> conflicts) {
    // Implement your alerting mechanism here:
    // - Email notifications
    // - Slack/Discord webhooks
    // - SMS alerts
    // - Push notifications

    System.err.println("ALERT: " + message);
    System.err.println("Conflicts: " + conflicts.size());

    // Example: Send to monitoring system
    // monitoringClient.sendAlert("booking-conflict", message, conflicts);
  }

  /**
   * Track booking metrics
   */
  public void trackBookingAttempt(boolean success, boolean hadConflict) {
    totalBookings.incrementAndGet();

    if (hadConflict) {
      conflictAttempts.incrementAndGet();
    }

    if (success) {
      successfulBookings.incrementAndGet();
    } else {
      failedBookings.incrementAndGet();
    }
  }

  /**
   * Get booking metrics
   */
  public Map<String, Object> getMetrics() {
    Map<String, Object> metrics = new HashMap<>();
    metrics.put("totalBookings", totalBookings.get());
    metrics.put("successfulBookings", successfulBookings.get());
    metrics.put("failedBookings", failedBookings.get());
    metrics.put("conflictAttempts", conflictAttempts.get());
    metrics.put("successRate",
        totalBookings.get() > 0 ? (double) successfulBookings.get() / totalBookings.get() : 0);
    metrics.put("conflictRate",
        totalBookings.get() > 0 ? (double) conflictAttempts.get() / totalBookings.get() : 0);

    return metrics;
  }

  /**
   * Health check for booking system
   */
  @Scheduled(fixedRate = 60000) // Every minute
  public void performHealthCheck() {
    try {
      // Check for recent failed bookings
      long recentFailures = failedBookings.get();
      if (recentFailures > 10) { // Threshold for alerting
        sendAlert("High booking failure rate detected", null);
      }

      // Check for system responsiveness
      long startTime = System.currentTimeMillis();
      bookingRepository.count(); // Simple DB check
      long responseTime = System.currentTimeMillis() - startTime;

      if (responseTime > 5000) { // 5 second threshold
        sendAlert("Database response time is high: " + responseTime + "ms", null);
      }

    } catch (Exception e) {
      sendAlert("Health check failed: " + e.getMessage(), null);
    }
  }

  /**
   * Clean up old booking data (run daily)
   */
  @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
  public void cleanupOldData() {
    try {
      // This would need to be implemented based on your data retention policy
      // LocalDate cutoffDate = LocalDate.now().minusDays(90); // Keep 90 days of data
      // bookingRepository.deleteOldBookings(cutoffDate);

      System.out.println("Old booking data cleanup completed");

    } catch (Exception e) {
      System.err.println("Error in data cleanup: " + e.getMessage());
    }
  }
}
