package com.zanar.playera.controller;

import com.zanar.playera.entity.Slot;
import com.zanar.playera.service.BookingService;
import com.zanar.playera.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@Tag(name = "Slot Management", description = "APIs for managing venue time slots and availability")
@CrossOrigin(origins = "*")
public class SlotController {
    
    @Autowired
    private SlotService slotService;
    
    @Autowired
    private BookingService bookingService;

    @GetMapping("/available/{courtId}")
    @Operation(summary = "Get available slots by court", description = "Get all available slots for a specific court")
    public ResponseEntity<List<Slot>> getAvailableSlots(@PathVariable Long courtId) {
        List<Slot> slots = slotService.getAvailableSlotsByCourt(courtId);
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/available/{courtId}/date/{date}")
    @Operation(summary = "Get available slots by court and date", description = "Get available slots for a specific court on a specific date")
    public ResponseEntity<List<Slot>> getAvailableSlotsByDate(
            @PathVariable Long courtId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = bookingService.getAvailableSlotsByCourt(courtId, date);
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/venue/{venueId}/date/{date}")
    @Operation(summary = "Get available slots by venue and date", description = "Get all available slots for a venue on a specific date")
    public ResponseEntity<List<Slot>> getAvailableSlotsByVenue(
            @PathVariable Long venueId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = bookingService.getAvailableSlotsByVenue(venueId, date);
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/check-availability/{courtId}")
    @Operation(summary = "Check slot availability", description = "Check if a specific time slot is available for booking")
    public ResponseEntity<Boolean> checkSlotAvailability(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        boolean isAvailable = bookingService.isSlotAvailable(courtId, date, startTime, endTime);
        return ResponseEntity.ok(isAvailable);
    }
    
    @GetMapping("/calendar/{courtId}")
    @Operation(summary = "Get court calendar", description = "Get all slots for a court within a date range")
    public ResponseEntity<List<Slot>> getCourtCalendar(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Slot> slots = slotService.getCourtCalendar(courtId, startDate, endDate);
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/court/{courtId}/status/{status}")
    @Operation(summary = "Get slots by status", description = "Get slots for a court with specific status")
    public ResponseEntity<List<Slot>> getSlotsByStatus(
            @PathVariable Long courtId,
            @PathVariable Slot.SlotStatus status) {
        List<Slot> slots = slotService.getSlotsByCourtAndStatus(courtId, status);
        return ResponseEntity.ok(slots);
    }
}
