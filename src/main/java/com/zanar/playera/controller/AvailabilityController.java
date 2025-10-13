package com.zanar.playera.controller;

import com.zanar.playera.service.BookingValidationService;
import com.zanar.playera.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = "*")
public class AvailabilityController {

  @Autowired
  private BookingValidationService bookingValidationService;

  @Autowired
  private ReservationService reservationService;

  /**
   * Check if a time slot is available for booking
   */
  @GetMapping("/check")
  public ResponseEntity<Map<String, Object>> checkAvailability(
      @RequestParam Long courtId,
      @RequestParam String date,
      @RequestParam String startTime,
      @RequestParam String endTime,
      @RequestParam(required = false) String customerId) {

    try {
      LocalDate bookingDate = LocalDate.parse(date);
      LocalTime start = LocalTime.parse(startTime);
      LocalTime end = LocalTime.parse(endTime);

      Map<String, Object> response = new HashMap<>();

      // Check basic availability
      boolean isAvailable = bookingValidationService.isTimeSlotAvailable(courtId, bookingDate, start, end);
      response.put("available", isAvailable);

      // Check reservation status if customer ID provided
      if (customerId != null) {
        boolean isReserved = !reservationService.isTimeSlotAvailableForReservation(courtId, bookingDate, start, end);
        response.put("reserved", isReserved);
      }

      response.put("courtId", courtId);
      response.put("date", date);
      response.put("startTime", startTime);
      response.put("endTime", endTime);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      errorResponse.put("available", false);
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Reserve a time slot temporarily
   */
  @PostMapping("/reserve")
  public ResponseEntity<Map<String, Object>> reserveTimeSlot(
      @RequestParam Long courtId,
      @RequestParam String date,
      @RequestParam String startTime,
      @RequestParam String endTime,
      @RequestParam String customerId) {

    try {
      LocalDate bookingDate = LocalDate.parse(date);
      LocalTime start = LocalTime.parse(startTime);
      LocalTime end = LocalTime.parse(endTime);

      var reservation = reservationService.createReservation(courtId, bookingDate, start, end, customerId);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("reservationId", reservation.getId());
      response.put("expiresAt", reservation.getExpiresAt());
      response.put("message", "Time slot reserved successfully");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Cancel a reservation
   */
  @PostMapping("/cancel-reservation")
  public ResponseEntity<Map<String, Object>> cancelReservation(@RequestParam Long reservationId) {
    try {
      reservationService.cancelReservation(reservationId);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Reservation cancelled successfully");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * Get available time slots for a court on a specific date
   */
  @GetMapping("/slots")
  public ResponseEntity<Map<String, Object>> getAvailableSlots(
      @RequestParam Long courtId,
      @RequestParam String date) {

    try {
      LocalDate bookingDate = LocalDate.parse(date);

      // This would typically call a service to get all available slots
      // For now, return a placeholder response
      Map<String, Object> response = new HashMap<>();
      response.put("courtId", courtId);
      response.put("date", date);
      response.put("slots", new Object[] {}); // Empty array for now

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }
}
