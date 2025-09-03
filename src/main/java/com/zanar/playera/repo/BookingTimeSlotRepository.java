package com.zanar.playera.repo;

import com.zanar.playera.entity.BookingTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingTimeSlotRepository extends JpaRepository<BookingTimeSlot, Long> {

  /**
   * Find all booking time slots for a specific court and date
   */
  @Query("SELECT bts FROM BookingTimeSlot bts WHERE bts.court.courtId = :courtId " +
      "AND DATE(bts.booking.bookingDate) = :date")
  List<BookingTimeSlot> findByCourtAndDate(@Param("courtId") Long courtId, @Param("date") LocalDate date);

  /**
   * Find conflicting booking time slots for a court on a specific date and time
   * range
   */
  @Query("SELECT bts FROM BookingTimeSlot bts WHERE bts.court.courtId = :courtId " +
      "AND DATE(bts.booking.bookingDate) = :date " +
      "AND ((bts.startTime < :endTime AND bts.endTime > :startTime))")
  List<BookingTimeSlot> findConflictingTimeSlots(
      @Param("courtId") Long courtId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime);

  /**
   * Find all booking time slots for a specific booking
   */
  List<BookingTimeSlot> findByBooking_BookingId(Long bookingId);
}
