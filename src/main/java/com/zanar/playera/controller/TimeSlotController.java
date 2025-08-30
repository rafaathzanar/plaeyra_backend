package com.zanar.playera.controller;

import com.zanar.playera.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TimeSlotController {

  private final TimeSlotService timeSlotService;

  /**
   * Get available time slots for a court on a specific date
   */
  @GetMapping("/court/{courtId}/date/{date}")
  public ResponseEntity<List<TimeSlotService.TimeSlotDTO>> getAvailableSlots(
      @PathVariable Long courtId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    try {
      List<TimeSlotService.TimeSlotDTO> slots = timeSlotService.generateAvailableSlots(courtId, date);
      return ResponseEntity.ok(slots);
    } catch (Exception e) {
      log.error("Error getting available slots for court {} on date {}", courtId, date, e);
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get available time slots for a court for a date range (for calendar view)
   */
  @GetMapping("/court/{courtId}/range")
  public ResponseEntity<Map<LocalDate, List<TimeSlotService.TimeSlotDTO>>> getSlotsForDateRange(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    try {
      Map<LocalDate, List<TimeSlotService.TimeSlotDTO>> slotsByDate = timeSlotService.getSlotsForDateRange(courtId,
          startDate, endDate);
      return ResponseEntity.ok(slotsByDate);
    } catch (Exception e) {
      log.error("Error getting slots for date range for court {}: {} to {}", courtId, startDate, endDate, e);
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Check if a specific time slot is available
   */
  @GetMapping("/court/{courtId}/availability")
  public ResponseEntity<Boolean> checkSlotAvailability(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime) {

    try {
      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      boolean isAvailable = timeSlotService.isSlotAvailable(courtId, date, start, end);
      return ResponseEntity.ok(isAvailable);
    } catch (Exception e) {
      log.error("Error checking slot availability for court {} on date {} from {} to {}",
          courtId, date, startTime, endTime, e);
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Block a time slot (for maintenance, reservations, etc.)
   */
  @PostMapping("/court/{courtId}/block")
  public ResponseEntity<String> blockTimeSlot(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime,
      @RequestParam String reason) {

    try {
      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      timeSlotService.blockTimeSlot(courtId, date, start, end, reason);
      return ResponseEntity.ok("Time slot blocked successfully");
    } catch (Exception e) {
      log.error("Error blocking time slot for court {} on date {} from {} to {}: {}",
          courtId, date, startTime, endTime, reason, e);
      return ResponseEntity.badRequest().body("Failed to block time slot: " + e.getMessage());
    }
  }

  /**
   * Unblock a time slot
   */
  @DeleteMapping("/court/{courtId}/unblock")
  public ResponseEntity<String> unblockTimeSlot(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime) {

    try {
      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      timeSlotService.unblockTimeSlot(courtId, date, start, end);
      return ResponseEntity.ok("Time slot unblocked successfully");
    } catch (Exception e) {
      log.error("Error unblocking time slot for court {} on date {} from {} to {}: {}",
          courtId, date, startTime, endTime, e);
      return ResponseEntity.badRequest().body("Failed to unblock time slot: " + e.getMessage());
    }
  }

  /**
   * Get peak hours for a court (for dynamic pricing)
   */
  @GetMapping("/court/{courtId}/peak-hours")
  public ResponseEntity<List<String>> getPeakHours(@PathVariable Long courtId) {
    try {
      List<java.time.LocalTime> peakHours = timeSlotService.getPeakHours(courtId);
      List<String> peakHoursFormatted = peakHours.stream()
          .map(time -> time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
          .toList();
      return ResponseEntity.ok(peakHoursFormatted);
    } catch (Exception e) {
      log.error("Error getting peak hours for court {}", courtId, e);
      return ResponseEntity.badRequest().build();
    }
  }
}
