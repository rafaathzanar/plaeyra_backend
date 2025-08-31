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

    LocalTime currentTime = court.getOpeningTime();
    LocalTime closingTime = court.getClosingTime();
    int slotDuration = court.getSlotDurationMinutes();

    while (currentTime.isBefore(closingTime)) {
      // Check if this time is during break time
      if (court.getHasBreakTime() && court.getBreakStartTime() != null && court.getBreakEndTime() != null) {
        if (currentTime.isAfter(court.getBreakStartTime()) && currentTime.isBefore(court.getBreakEndTime())) {
          currentTime = currentTime.plusMinutes(slotDuration);
          continue;
        }
      }

      // Check if court is available on this day
      DayOfWeek dayOfWeek = date.getDayOfWeek();
      if (court.getAvailabilitySchedule().containsKey(dayOfWeek)) {
        var availability = court.getAvailabilitySchedule().get(dayOfWeek);
        if (availability != null && availability.getIsAvailable()) {
          Slot slot = new Slot();
          slot.setCourt(court);
          slot.setDate(date);
          slot.setStartTime(currentTime);
          slot.setEndTime(currentTime.plusMinutes(slotDuration));
          slot.setStatus(Slot.SlotStatus.AVAILABLE);
          slots.add(slot);
        }
      }

      currentTime = currentTime.plusMinutes(slotDuration);
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
