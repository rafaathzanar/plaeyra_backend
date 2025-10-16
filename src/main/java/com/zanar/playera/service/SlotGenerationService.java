package com.zanar.playera.service;

import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Slot;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotGenerationService {

  private final CourtRepository courtRepository;
  private final SlotRepository slotRepository;

  /**
   * Generate time slots for a court on a specific date
   */
  public List<Slot> generateSlotsForDate(Long courtId, LocalDate date) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    List<Slot> slots = new ArrayList<>();

    if (court.getOpeningTime() == null || court.getClosingTime() == null ||
        court.getSlotDurationMinutes() == null) {
      log.warn("Court {} does not have proper time configuration", courtId);
      return slots;
    }

    // Check if court is available on this date (weekends/holidays)
    if (!isCourtAvailableOnDate(court, date)) {
      log.info("Court {} is not available on date {} (weekend/holiday check)", courtId, date);
      return slots;
    }

    LocalTime currentTime = court.getOpeningTime();
    LocalTime closingTime = court.getClosingTime();
    int slotDuration = court.getSlotDurationMinutes();

    log.info("Generating slots for court {} on {} from {} to {} with {} minute duration",
        courtId, date, currentTime, closingTime, slotDuration);

    while (currentTime.isBefore(closingTime)) {
      LocalTime endTime = currentTime.plusMinutes(slotDuration);

      // Skip if end time exceeds closing time
      if (endTime.isAfter(closingTime)) {
        break;
      }

      // Check if this time is during break time
      if (!isDuringBreakTime(court, currentTime, endTime)) {
        // Check if slot already exists to avoid duplicates
        List<Slot> existingSlots = slotRepository.findByCourt_CourtIdAndDateAndStartTimeAndEndTime(
            courtId, date, currentTime, endTime);

        if (existingSlots.isEmpty()) {
          Slot slot = new Slot();
          slot.setCourt(court);
          slot.setDate(date);
          slot.setStartTime(currentTime);
          slot.setEndTime(endTime);
          slot.setStatus(Slot.SlotStatus.AVAILABLE);
          slots.add(slot);

          log.debug("Created slot: {} - {} for court {} on {}", currentTime, endTime, courtId, date);
        } else {
          log.debug("Slot already exists: {} - {} for court {} on {}", currentTime, endTime, courtId, date);
        }
      }

      currentTime = endTime;
    }

    // Save all generated slots to database
    if (!slots.isEmpty()) {
      List<Slot> savedSlots = slotRepository.saveAll(slots);
      log.info("Generated and saved {} slots for court {} on {}", savedSlots.size(), courtId, date);
    } else {
      log.warn("No slots generated for court {} on {}", courtId, date);
    }

    return slots;
  }

  /**
   * Generate slots for a date range
   */
  public List<Slot> generateSlotsForDateRange(Long courtId, LocalDate startDate, LocalDate endDate) {
    List<Slot> allSlots = new ArrayList<>();
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      List<Slot> dailySlots = generateSlotsForDate(courtId, currentDate);
      allSlots.addAll(dailySlots);
      currentDate = currentDate.plusDays(1);
    }

    return allSlots;
  }

  /**
   * Check if court is available on a specific date
   */
  private boolean isCourtAvailableOnDate(Court court, LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    // Check if court is active on weekends
    if ((dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) &&
        !Boolean.TRUE.equals(court.getIsActiveOnWeekends())) {
      return false;
    }

    // Check if court is active on holidays (you can implement holiday checking
    // logic)
    if (isHoliday(date) && !Boolean.TRUE.equals(court.getIsActiveOnHolidays())) {
      return false;
    }

    // Check if court status is active
    return Court.CourtStatus.ACTIVE.equals(court.getStatus());
  }

  /**
   * Check if a time slot is during break time
   */
  private boolean isDuringBreakTime(Court court, LocalTime startTime, LocalTime endTime) {
    if (!Boolean.TRUE.equals(court.getHasBreakTime()) ||
        court.getBreakStartTime() == null || court.getBreakEndTime() == null) {
      return false;
    }

    // Check if the slot overlaps with break time
    return !(endTime.isBefore(court.getBreakStartTime()) ||
        startTime.isAfter(court.getBreakEndTime()));
  }

  /**
   * Check if a date is a holiday (you can implement your own holiday logic)
   */
  private boolean isHoliday(LocalDate date) {
    // Simple holiday checking - you can enhance this with a holiday API or database
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    // Example holidays (Sri Lanka)
    return (month == 1 && day == 1) || // New Year
        (month == 4 && day == 13) || // Sinhala & Tamil New Year
        (month == 5 && day == 1) || // May Day
        (month == 12 && day == 25); // Christmas
  }

  /**
   * Check if a specific time slot is available
   */
  public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // Check if court is open at this time
    if (!court.isOpenAt(startTime) || !court.isOpenAt(endTime)) {
      return false;
    }

    // Check if time is during break
    if (court.getHasBreakTime() && court.getBreakStartTime() != null && court.getBreakEndTime() != null) {
      if (startTime.isBefore(court.getBreakEndTime()) && endTime.isAfter(court.getBreakStartTime())) {
        return false;
      }
    }

    // Check if slot is already booked
    List<Slot> existingSlots = slotRepository.findOverlappingSlots(
        courtId, date, startTime, endTime);

    return existingSlots.stream().allMatch(slot -> slot.getStatus() == Slot.SlotStatus.AVAILABLE);
  }
}
