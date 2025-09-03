package com.zanar.playera.service;

import com.zanar.playera.entity.Booking;
import com.zanar.playera.repo.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyReminderService {

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private NotificationService notificationService;

  // Run every day at 8:00 AM
  @Scheduled(cron = "0 0 8 * * ?")
  public void createDailyBookingReminders() {
    LocalDate today = LocalDate.now();

    // Find all bookings for today
    List<Booking> todaysBookings = bookingRepository.findAll().stream()
        .filter(booking -> booking.getBookingDate() != null)
        .filter(booking -> booking.getBookingDate().toLocalDate().equals(today))
        .filter(booking -> "CONFIRMED".equals(booking.getBookingStatus()))
        .collect(java.util.stream.Collectors.toList());

    // Create reminder notifications for each booking
    for (Booking booking : todaysBookings) {
      try {
        notificationService.createBookingReminderNotification(booking);
      } catch (Exception e) {
        // Log error but continue with other bookings
        System.err.println("Error creating reminder for booking " + booking.getBookingId() + ": " + e.getMessage());
      }
    }

    System.out.println("Created " + todaysBookings.size() + " daily booking reminders for " + today);
  }
}
