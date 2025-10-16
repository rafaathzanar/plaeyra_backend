package com.zanar.playera.service;

import com.zanar.playera.entity.SlotReservation;
import com.zanar.playera.entity.Court;
import com.zanar.playera.repo.SlotReservationRepository;
import com.zanar.playera.repo.CourtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

  @Autowired
  private SlotReservationRepository reservationRepository;

  @Autowired
  private CourtRepository courtRepository;

  private static final int RESERVATION_TIMEOUT_MINUTES = 5; // 5 minutes to complete booking

  /**
   * Create a temporary reservation for a time slot
   */
  public SlotReservation createReservation(Long courtId, LocalDate date, LocalTime startTime,
      LocalTime endTime, String customerId) {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiresAt = now.plusMinutes(RESERVATION_TIMEOUT_MINUTES);

    // Check if slot is already reserved by another customer
    if (reservationRepository.isTimeSlotReservedByAnotherCustomer(
        courtId, date, startTime, endTime, customerId, now)) {
      throw new IllegalArgumentException("Time slot is currently reserved by another customer");
    }

    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new IllegalArgumentException("Court not found"));

    SlotReservation reservation = new SlotReservation();
    reservation.setCourt(court);
    reservation.setDate(date);
    reservation.setStartTime(startTime);
    reservation.setEndTime(endTime);
    reservation.setCustomerId(customerId);
    reservation.setReservedAt(now);
    reservation.setExpiresAt(expiresAt);
    reservation.setStatus(SlotReservation.ReservationStatus.ACTIVE);

    return reservationRepository.save(reservation);
  }

  /**
   * Confirm a reservation (convert to actual booking)
   */
  public void confirmReservation(Long reservationId) {
    SlotReservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    if (reservation.isExpired()) {
      throw new IllegalArgumentException("Reservation has expired");
    }

    reservation.confirm();
    reservationRepository.save(reservation);
  }

  /**
   * Cancel a reservation
   */
  public void cancelReservation(Long reservationId) {
    SlotReservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    reservation.cancel();
    reservationRepository.save(reservation);
  }

  /**
   * Check if a time slot is available for reservation
   */
  public boolean isTimeSlotAvailableForReservation(Long courtId, LocalDate date,
      LocalTime startTime, LocalTime endTime) {
    List<SlotReservation> activeReservations = reservationRepository.findActiveReservations(
        courtId, date, startTime, endTime, LocalDateTime.now());

    return activeReservations.isEmpty();
  }

  /**
   * Clean up expired reservations (runs every minute)
   */
  @Scheduled(fixedRate = 60000) // Every minute
  public void cleanupExpiredReservations() {
    LocalDateTime now = LocalDateTime.now();
    int deletedCount = reservationRepository.deleteExpiredReservations(now);

    if (deletedCount > 0) {
      System.out.println("Cleaned up " + deletedCount + " expired reservations");
    }
  }

  /**
   * Get active reservations for a customer
   */
  public List<SlotReservation> getActiveReservationsForCustomer(String customerId) {
    return reservationRepository.findByCustomerIdAndStatus(
        customerId, SlotReservation.ReservationStatus.ACTIVE);
  }
}
