package com.zanar.playera.service;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.entity.*;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class BookingValidationService {

  @Autowired
  private BookingTimeSlotRepository bookingTimeSlotRepository;

  @Autowired
  private SlotRepository slotRepository;

  @Autowired
  private CourtRepository courtRepository;

  @Autowired
  private EquipmentRepository equipmentRepository;

  // Per-court locking mechanism to prevent concurrent bookings
  private final Map<Long, ReentrantLock> courtLocks = new ConcurrentHashMap<>();

  /**
   * Comprehensive validation before creating a booking
   */
  public void validateBookingRequest(BookingRequestDTO dto) {
    // 1. Validate basic requirements
    validateBasicRequirements(dto);

    // 2. Validate court availability
    validateCourtAvailability(dto);

    // 3. Validate equipment availability
    validateEquipmentAvailability(dto);

    // 4. Validate time slot conflicts
    validateTimeSlotConflicts(dto);

    // 5. Validate business rules
    validateBusinessRules(dto);
  }

  /**
   * Validate basic booking requirements
   */
  private void validateBasicRequirements(BookingRequestDTO dto) {
    if (dto.getBookingDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("Booking date cannot be in the past");
    }

    if (dto.getCourtBookings() == null || dto.getCourtBookings().isEmpty()) {
      throw new IllegalArgumentException("At least one court booking is required");
    }

    // Validate time ranges
    if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
      for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
        if (!range.isValidTimeRange()) {
          throw new IllegalArgumentException("Invalid time range: " +
              range.getStartTime() + " - " + range.getEndTime());
        }
      }
    } else if (!dto.isValidTimeRange()) {
      throw new IllegalArgumentException("Invalid time range: start time must be before end time");
    }
  }

  /**
   * Validate court availability with locking
   */
  private void validateCourtAvailability(BookingRequestDTO dto) {
    for (BookingRequestDTO.CourtBookingDTO courtBooking : dto.getCourtBookings()) {
      Long courtId = courtBooking.getCourtId();

      // Acquire lock for this court
      ReentrantLock courtLock = courtLocks.computeIfAbsent(courtId, k -> new ReentrantLock());
      courtLock.lock();

      try {
        Court court = courtRepository.findById(courtId)
            .orElseThrow(() -> new IllegalArgumentException("Court not found: " + courtId));

        if (court.getStatus() != Court.CourtStatus.ACTIVE) {
          throw new IllegalArgumentException("Court " + court.getCourtName() + " is not available");
        }

        // Check if court is in maintenance mode
        if (court.getMaintenanceMode() != null && court.getMaintenanceMode()) {
          throw new IllegalArgumentException("Court " + court.getCourtName() + " is under maintenance");
        }

      } finally {
        courtLock.unlock();
      }
    }
  }

  /**
   * Validate equipment availability
   */
  private void validateEquipmentAvailability(BookingRequestDTO dto) {
    if (dto.getEquipmentBookings() != null) {
      for (BookingRequestDTO.EquipmentBookingDTO equipmentBooking : dto.getEquipmentBookings()) {
        Equipment equipment = equipmentRepository.findById(equipmentBooking.getEquipmentId())
            .orElseThrow(
                () -> new IllegalArgumentException("Equipment not found: " + equipmentBooking.getEquipmentId()));

        if (equipment.getAvailableQuantity() < equipmentBooking.getQuantity()) {
          throw new IllegalArgumentException("Insufficient equipment quantity for " + equipment.getName() +
              ". Available: " + equipment.getAvailableQuantity() + ", Requested: " + equipmentBooking.getQuantity());
        }
      }
    }
  }

  /**
   * Validate time slot conflicts with enhanced checking
   */
  private void validateTimeSlotConflicts(BookingRequestDTO dto) {
    if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
      for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
        validateTimeRangeConflicts(dto.getCourtBookings().get(0).getCourtId(),
            dto.getBookingDate(), range.getStartTime(), range.getEndTime());
      }
    } else {
      validateTimeRangeConflicts(dto.getCourtBookings().get(0).getCourtId(),
          dto.getBookingDate(), dto.getStartTime(), dto.getEndTime());
    }
  }

  /**
   * Check for conflicts in a specific time range
   */
  private void validateTimeRangeConflicts(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    // Check BookingTimeSlot conflicts
    List<BookingTimeSlot> conflictingBookingSlots = bookingTimeSlotRepository.findConflictingTimeSlots(
        courtId, date, startTime, endTime);

    if (!conflictingBookingSlots.isEmpty()) {
      StringBuilder conflictDetails = new StringBuilder();
      for (BookingTimeSlot slot : conflictingBookingSlots) {
        conflictDetails.append(String.format("Booking %d: %s - %s; ",
            slot.getBooking().getBookingId(), slot.getStartTime(), slot.getEndTime()));
      }
      throw new IllegalArgumentException("Time slot conflict detected: " + conflictDetails.toString());
    }

    // Check Slot conflicts
    List<Slot> conflictingSlots = slotRepository.findConflictingSlots(courtId, date, startTime, endTime);

    if (!conflictingSlots.isEmpty()) {
      StringBuilder conflictDetails = new StringBuilder();
      for (Slot slot : conflictingSlots) {
        conflictDetails.append(String.format("Slot %d (%s): %s - %s; ",
            slot.getId(), slot.getStatus(), slot.getStartTime(), slot.getEndTime()));
      }
      throw new IllegalArgumentException("Slot conflict detected: " + conflictDetails.toString());
    }
  }

  /**
   * Validate business rules
   */
  private void validateBusinessRules(BookingRequestDTO dto) {
    for (BookingRequestDTO.CourtBookingDTO courtBooking : dto.getCourtBookings()) {
      Court court = courtRepository.findById(courtBooking.getCourtId()).orElse(null);
      if (court == null)
        continue;

      // Check minimum booking duration
      if (court.getMinBookingDuration() != null &&
          courtBooking.getTimeDuration() < court.getMinBookingDuration()) {
        throw new IllegalArgumentException("Booking duration must be at least " +
            court.getMinBookingDuration() + " hours for court " + court.getCourtName());
      }

      // Check maximum booking duration
      if (court.getMaxBookingDuration() != null &&
          courtBooking.getTimeDuration() > court.getMaxBookingDuration()) {
        throw new IllegalArgumentException("Booking duration cannot exceed " +
            court.getMaxBookingDuration() + " hours for court " + court.getCourtName());
      }

      // Check court operating hours
      validateOperatingHours(court, dto);
    }
  }

  /**
   * Validate booking against court operating hours
   */
  private void validateOperatingHours(Court court, BookingRequestDTO dto) {
    if (court.getOpeningTime() != null && court.getClosingTime() != null) {
      LocalTime bookingStart = dto.getStartTime();
      LocalTime bookingEnd = dto.getEndTime();

      if (bookingStart.isBefore(court.getOpeningTime()) || bookingEnd.isAfter(court.getClosingTime())) {
        throw new IllegalArgumentException("Booking time is outside court operating hours (" +
            court.getOpeningTime() + " - " + court.getClosingTime() + ")");
      }
    }

    // Check break times
    if (court.getHasBreakTime() != null && court.getHasBreakTime() &&
        court.getBreakStartTime() != null && court.getBreakEndTime() != null) {

      LocalTime bookingStart = dto.getStartTime();
      LocalTime bookingEnd = dto.getEndTime();

      if (bookingStart.isBefore(court.getBreakEndTime()) && bookingEnd.isAfter(court.getBreakStartTime())) {
        throw new IllegalArgumentException("Booking time conflicts with court break time (" +
            court.getBreakStartTime() + " - " + court.getBreakEndTime() + ")");
      }
    }
  }

  /**
   * Release court lock (called after booking completion)
   */
  public void releaseCourtLock(Long courtId) {
    ReentrantLock courtLock = courtLocks.get(courtId);
    if (courtLock != null && courtLock.isHeldByCurrentThread()) {
      courtLock.unlock();
    }
  }

  /**
   * Check if a specific time slot is available
   */
  public boolean isTimeSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    try {
      validateTimeRangeConflicts(courtId, date, startTime, endTime);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
