package com.zanar.playera.repo;

import com.zanar.playera.entity.SlotReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotReservationRepository extends JpaRepository<SlotReservation, Long> {

  /**
   * Find active reservations for a court and time range
   */
  @Query("SELECT sr FROM SlotReservation sr WHERE sr.court.courtId = :courtId " +
      "AND sr.date = :date " +
      "AND sr.status = 'ACTIVE' " +
      "AND sr.expiresAt > :currentTime " +
      "AND ((sr.startTime < :endTime AND sr.endTime > :startTime))")
  List<SlotReservation> findActiveReservations(
      @Param("courtId") Long courtId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime,
      @Param("currentTime") LocalDateTime currentTime);

  /**
   * Find reservations by customer
   */
  List<SlotReservation> findByCustomerIdAndStatus(String customerId, SlotReservation.ReservationStatus status);

  /**
   * Find expired reservations
   */
  @Query("SELECT sr FROM SlotReservation sr WHERE sr.expiresAt < :currentTime " +
      "AND sr.status = 'ACTIVE'")
  List<SlotReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

  /**
   * Delete expired reservations
   */
  @Modifying
  @Query("DELETE FROM SlotReservation sr WHERE sr.expiresAt < :currentTime " +
      "AND sr.status = 'ACTIVE'")
  int deleteExpiredReservations(@Param("currentTime") LocalDateTime currentTime);

  /**
   * Check if a specific time slot is reserved by another customer
   */
  @Query("SELECT COUNT(sr) > 0 FROM SlotReservation sr WHERE sr.court.courtId = :courtId " +
      "AND sr.date = :date " +
      "AND sr.status = 'ACTIVE' " +
      "AND sr.expiresAt > :currentTime " +
      "AND sr.customerId != :customerId " +
      "AND ((sr.startTime < :endTime AND sr.endTime > :startTime))")
  boolean isTimeSlotReservedByAnotherCustomer(
      @Param("courtId") Long courtId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime,
      @Param("customerId") String customerId,
      @Param("currentTime") LocalDateTime currentTime);
}
