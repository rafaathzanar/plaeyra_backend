package com.zanar.playera.repo;

import com.zanar.playera.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    
    // Find available slots by court and date
    List<Slot> findByCourtIdAndDateAndStatus(Long courtId, LocalDate date, Slot.SlotStatus status);
    
    // Find available slots by court, date, and time range
    @Query("SELECT s FROM Slot s WHERE s.court.id = :courtId AND s.date = :date " +
           "AND s.status = :status AND s.startTime >= :startTime AND s.endTime <= :endTime")
    List<Slot> findAvailableSlotsByCourtAndDateTimeRange(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("status") Slot.SlotStatus status,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
    
    // Find slots by court and date range
    List<Slot> findByCourtIdAndDateBetween(Long courtId, LocalDate startDate, LocalDate endDate);
    
    // Find all available slots for a venue on a specific date
    @Query("SELECT s FROM Slot s WHERE s.court.venue.venueId = :venueId AND s.date = :date AND s.status = :status")
    List<Slot> findAvailableSlotsByVenueAndDate(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("status") Slot.SlotStatus status);
    
    // Find conflicting slots for a booking
    @Query("SELECT s FROM Slot s WHERE s.court.id = :courtId AND s.date = :date " +
           "AND s.status IN ('BOOKED', 'RESERVED') " +
           "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    List<Slot> findConflictingSlots(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
    
    // Find slots by booking
    List<Slot> findByBookingId(Long bookingId);
    
    // Count available slots by court and date
    long countByCourtIdAndDateAndStatus(Long courtId, LocalDate date, Slot.SlotStatus status);
    
    // Find slots that need to be released (past date)
    @Query("SELECT s FROM Slot s WHERE s.date < :currentDate AND s.status = 'BOOKED'")
    List<Slot> findExpiredBookedSlots(@Param("currentDate") LocalDate currentDate);
    
    // Legacy method for backward compatibility
    List<Slot> findByCourtIdAndStatus(Long courtId, Slot.SlotStatus status);
}
