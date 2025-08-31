package com.zanar.playera.controller;

import com.zanar.playera.service.SlotGenerationService;
import com.zanar.playera.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TimeSlotController {

  private final TimeSlotService timeSlotService;
  private final SlotGenerationService slotGenerationService;

  /**
   * Get available time slots for a court on a specific date
   */
  @GetMapping("/court/{courtId}/date/{date}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
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
   * Get all time slots for a court on a specific date (including blocked ones)
   * This is used by the venue owner dashboard to show all slot statuses
   */
  @GetMapping("/court/{courtId}/date/{date}/all")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<List<TimeSlotService.TimeSlotDTO>> getAllTimeSlots(
      @PathVariable Long courtId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    try {
      List<TimeSlotService.TimeSlotDTO> slots = timeSlotService.getAllTimeSlotsForDate(courtId, date);
      return ResponseEntity.ok(slots);
    } catch (Exception e) {
      log.error("Error getting all time slots for court {} on date {}", courtId, date, e);
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Get available time slots for a court for a date range (for calendar view)
   */
  @GetMapping("/court/{courtId}/range")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
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
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
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
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> blockTimeSlot(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime,
      @RequestParam String reason,
      @RequestParam(defaultValue = "false") boolean isMaintenance) {

    try {
      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      timeSlotService.blockTimeSlot(courtId, date, start, end, reason, isMaintenance);

      Map<String, String> response = new HashMap<>();
      response.put("message", "Time slot blocked successfully");
      response.put("status", "SUCCESS");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error blocking time slot for court {} on date {} from {} to {}: {}",
          courtId, date, startTime, endTime, reason, e);

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("message", "Failed to block time slot: " + e.getMessage());
      errorResponse.put("status", "ERROR");

      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Block recurring time slots
   */
  @PostMapping("/court/{courtId}/block-recurring")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> blockRecurringTimeSlots(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime,
      @RequestParam String reason,
      @RequestParam(defaultValue = "false") boolean isMaintenance,
      @RequestParam List<Integer> recurringDays) {

    try {
      log.info(
          "Received recurring block request - Court: {}, Dates: {} to {}, Time: {} to {}, Reason: {}, Maintenance: {}, Days: {}",
          courtId, startDate, endDate, startTime, endTime, reason, isMaintenance, recurringDays);

      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      timeSlotService.blockRecurringTimeSlots(courtId, startDate, endDate, start, end, reason, isMaintenance,
          recurringDays);

      Map<String, String> response = new HashMap<>();
      response.put("message", "Recurring time slots blocked successfully");
      response.put("status", "SUCCESS");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error blocking recurring time slots for court {} from {} to {}: {}",
          courtId, startDate, endDate, reason, e);

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("message", "Failed to block recurring time slots: " + e.getMessage());
      errorResponse.put("status", "ERROR");

      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Unblock a time slot
   */
  @DeleteMapping("/court/{courtId}/unblock")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> unblockTimeSlot(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) String endTime) {

    try {
      java.time.LocalTime start = java.time.LocalTime.parse(startTime);
      java.time.LocalTime end = java.time.LocalTime.parse(endTime);

      timeSlotService.unblockTimeSlot(courtId, date, start, end);

      Map<String, String> response = new HashMap<>();
      response.put("message", "Time slot unblocked successfully");
      response.put("status", "SUCCESS");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error unblocking time slot for court {} on date {} from {} to {}",
          courtId, date, startTime, endTime, e);

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("message", "Failed to unblock time slot: " + e.getMessage());
      errorResponse.put("status", "ERROR");

      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Get peak hours for a court (for dynamic pricing)
   */
  @GetMapping("/court/{courtId}/peak-hours")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<List<LocalTime>> getPeakHours(@PathVariable Long courtId) {
    try {
      List<LocalTime> peakHours = timeSlotService.getPeakHours(courtId);
      return ResponseEntity.ok(peakHours);
    } catch (Exception e) {
      log.error("Error getting peak hours for court {}", courtId, e);
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Manually generate time slots for a court (for testing/debugging)
   */
  @PostMapping("/court/{courtId}/generate-slots")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> generateTimeSlots(
      @PathVariable Long courtId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    try {
      slotGenerationService.generateSlotsForDateRange(courtId, startDate, endDate);

      Map<String, String> response = new HashMap<>();
      response.put("message", "Time slots generated successfully");
      response.put("status", "SUCCESS");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error generating time slots for court {} from {} to {}", courtId, startDate, endDate, e);

      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("message", "Failed to generate time slots: " + e.getMessage());
      errorResponse.put("status", "ERROR");

      return ResponseEntity.badRequest().body(errorResponse);
    }
  }
}
