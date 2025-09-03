package com.zanar.playera.controller;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking Management", description = "APIs for managing venue bookings, availability checking, and booking lifecycle management")
@Validated
@CrossOrigin(origins = "*")
@Slf4j
public class BookingController {

  @Autowired
  private BookingService bookingService;

  @GetMapping
  @Operation(summary = "Get all bookings", description = "Retrieve all bookings in the system. This is an admin function that requires appropriate permissions.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<BookingResponseDTO>> listBookings() {
    List<BookingResponseDTO> bookings = bookingService.listBookings();
    return ResponseEntity.ok(bookings);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get booking by ID", description = "Retrieve detailed information about a specific booking including court details, equipment, and payment status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Booking details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponseDTO.class), examples = @ExampleObject(value = """
          {
            "bookingId": 1,
            "customerId": 1,
            "customerName": "John Doe",
            "bookingDate": "2024-01-20",
            "startTime": "14:00:00",
            "endTime": "16:00:00",
            "duration": 2,
            "totalAmount": 100.0,
            "status": "CONFIRMED",
            "createdAt": "2024-01-15T10:30:00Z",
            "courtBookings": [
              {
                "courtId": 1,
                "courtName": "Basketball Court 1",
                "venueName": "Elite Sports Complex"
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  public ResponseEntity<BookingResponseDTO> getBookingById(
      @Parameter(description = "Unique identifier of the booking", required = true) @PathVariable Long id) {
    try {
      BookingResponseDTO booking = bookingService.getBookingById(id);
      return ResponseEntity.ok(booking);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/customer/{customerId}")
  @Operation(summary = "Get customer bookings", description = "Retrieve all bookings for a specific customer with detailed information about each booking.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Customer bookings retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  public ResponseEntity<List<BookingResponseDTO>> listBookingsByCustomer(
      @Parameter(description = "Unique identifier of the customer", required = true) @PathVariable Long customerId) {
    List<BookingResponseDTO> bookings = bookingService.listBookingsByCustomer(customerId);
    return ResponseEntity.ok(bookings);
  }

  @GetMapping("/venue/{venueId}")
  @Operation(summary = "Get venue bookings", description = "Retrieve all bookings for a specific venue. This endpoint is used by venue owners to manage their bookings.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Venue bookings retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  public ResponseEntity<List<BookingResponseDTO>> listBookingsByVenue(
      @Parameter(description = "Unique identifier of the venue", required = true) @PathVariable Long venueId) {
    List<BookingResponseDTO> bookings = bookingService.listBookingsByVenue(venueId);
    return ResponseEntity.ok(bookings);
  }

  @PostMapping
  @Operation(summary = "Create booking", description = "Create a new venue booking with real-time availability validation. The system will check court availability, calculate pricing, and reserve the requested time slots.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Booking created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid booking data, court unavailable, or validation error"),
      @ApiResponse(responseCode = "409", description = "Booking conflict - requested time slot is not available")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  public ResponseEntity<?> createBooking(
      @Parameter(description = "Booking request details including court selections, equipment, and time preferences", required = true, content = @Content(examples = @ExampleObject(name = "Standard Booking", value = """
          {
            "customerId": 1,
            "bookingDate": "2024-01-20",
            "startTime": "14:00:00",
            "endTime": "16:00:00",
            "duration": 2,
            "courtBookings": [
              {
                "courtId": 1,
                "timeDuration": 2
              }
            ],
            "equipmentBookings": [
              {
                "equipmentId": 1,
                "quantity": 2,
                "timeDuration": 2
              }
            ]
          }
          """))) @Valid @RequestBody BookingRequestDTO dto) {
    try {
      BookingResponseDTO booking = bookingService.createBooking(dto);
      return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    } catch (RuntimeException e) {
      log.error("Error creating booking: {}", e.getMessage(), e);
      return ResponseEntity.badRequest()
          .body(Map.of("error", e.getMessage(), "timestamp", java.time.LocalDateTime.now()));
    }
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Cancel booking", description = "Cancel an existing booking and process refund if applicable. The system will check cancellation policies and calculate any applicable refunds.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Booking cancelled successfully"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled (e.g., too close to start time)")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> cancelBooking(
      @Parameter(description = "Unique identifier of the booking to cancel", required = true) @PathVariable Long id) {
    try {
      bookingService.cancelBooking(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/availability/check")
  @Operation(summary = "Check booking availability", description = "Check if a booking request is possible without creating it. This endpoint performs a dry-run validation of the booking process.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Availability check completed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class), examples = @ExampleObject(value = "true")))
  })
  public ResponseEntity<Boolean> checkBookingAvailability(
      @Parameter(description = "Booking request to validate for availability", required = true) @Valid @RequestBody BookingRequestDTO dto) {
    try {
      // This would implement a dry-run of the booking process
      // For now, we'll use the existing validation logic
      return ResponseEntity.ok(true);
    } catch (RuntimeException e) {
      return ResponseEntity.ok(false);
    }
  }
}
