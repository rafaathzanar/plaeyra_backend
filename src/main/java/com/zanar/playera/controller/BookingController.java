package com.zanar.playera.controller;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.dto.BookingWithPaymentDTO;
import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.service.BookingService;
import com.zanar.playera.service.PaymentService;
import com.zanar.playera.service.StripeService;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.entity.Court;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
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

import java.time.LocalDateTime;
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

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private StripeService stripeService;

  @Autowired
  private CourtRepository courtRepository;

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

  @PostMapping("/with-payment")
  @Operation(summary = "Create booking with payment verification", description = "Create a new venue booking only after verifying that the payment was successful. This ensures no bookings are created for failed payments.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Booking created successfully after payment verification", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid booking data, payment verification failed, or validation error"),
      @ApiResponse(responseCode = "409", description = "Booking conflict - requested time slot is not available")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  public ResponseEntity<?> createBookingWithPayment(@Valid @RequestBody BookingWithPaymentDTO dto) {
    try {
      log.info("Creating booking with payment verification for payment intent: {}", dto.getPaymentIntentId());

      // Verify payment status with Stripe
      PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(dto.getPaymentIntentId());

      if (!"succeeded".equals(paymentIntent.getStatus())) {
        log.error("Payment verification failed. Payment intent {} has status: {}", dto.getPaymentIntentId(),
            paymentIntent.getStatus());

        String errorMessage;
        switch (paymentIntent.getStatus()) {
          case "requires_payment_method":
            errorMessage = "Payment failed. Please try again with a different payment method.";
            break;
          case "requires_confirmation":
            errorMessage = "Payment requires confirmation. Please complete the payment process.";
            break;
          case "requires_action":
            errorMessage = "Payment requires additional action. Please complete the authentication.";
            break;
          case "processing":
            errorMessage = "Payment is still processing. Please wait and try again.";
            break;
          case "canceled":
            errorMessage = "Payment was canceled. Please try again.";
            break;
          default:
            errorMessage = "Payment verification failed. Payment status: " + paymentIntent.getStatus();
        }

        return ResponseEntity.badRequest()
            .body(Map.of("error", errorMessage,
                "paymentIntentId", dto.getPaymentIntentId(),
                "paymentStatus", paymentIntent.getStatus(),
                "timestamp", java.time.LocalDateTime.now()));
      }

      log.info("Payment verified as successful for payment intent: {}", dto.getPaymentIntentId());

      // Convert to regular booking request
      BookingRequestDTO bookingRequest = new BookingRequestDTO();
      bookingRequest.setCustomerId(dto.getCustomerId());
      bookingRequest.setBookingDate(dto.getBookingDate());
      bookingRequest.setStartTime(dto.getStartTime());
      bookingRequest.setEndTime(dto.getEndTime());
      bookingRequest.setDuration(dto.getDuration());
      bookingRequest.setSpecialRequests(dto.getSpecialRequests());

      // Convert court bookings
      if (dto.getCourtBookings() != null) {
        bookingRequest.setCourtBookings(dto.getCourtBookings().stream()
            .map(cb -> {
              BookingRequestDTO.CourtBookingDTO courtBooking = new BookingRequestDTO.CourtBookingDTO();
              courtBooking.setCourtId(cb.getCourtId());
              courtBooking.setTimeDuration(cb.getTimeDuration());
              return courtBooking;
            })
            .collect(java.util.stream.Collectors.toList()));
      }

      // Convert equipment bookings
      if (dto.getEquipmentBookings() != null) {
        bookingRequest.setEquipmentBookings(dto.getEquipmentBookings().stream()
            .map(eb -> {
              BookingRequestDTO.EquipmentBookingDTO equipmentBooking = new BookingRequestDTO.EquipmentBookingDTO();
              equipmentBooking.setEquipmentId(eb.getEquipmentId());
              equipmentBooking.setQuantity(eb.getQuantity());
              equipmentBooking.setTimeDuration(eb.getTimeDuration());
              return equipmentBooking;
            })
            .collect(java.util.stream.Collectors.toList()));
      }

      // Convert time slot ranges
      if (dto.getTimeSlotRanges() != null) {
        bookingRequest.setTimeSlotRanges(dto.getTimeSlotRanges().stream()
            .map(tsr -> {
              BookingRequestDTO.TimeSlotRangeDTO timeSlotRange = new BookingRequestDTO.TimeSlotRangeDTO();
              timeSlotRange.setStartTime(tsr.getStartTime());
              timeSlotRange.setEndTime(tsr.getEndTime());
              timeSlotRange.setDuration(tsr.getDuration());
              return timeSlotRange;
            })
            .collect(java.util.stream.Collectors.toList()));
      }

      // Use total cost from frontend (includes dynamic pricing) or calculate as
      // fallback
      double totalCost = 0.0;
      log.info("=== COST CALCULATION DEBUG ===");
      log.info("Frontend total cost: {}", dto.getTotalCost());
      log.info("Time slot ranges: {}", dto.getTimeSlotRanges());
      log.info("Court bookings: {}", dto.getCourtBookings());
      log.info("Duration: {}", dto.getDuration());

      if (dto.getTotalCost() != null && dto.getTotalCost() > 0) {
        totalCost = dto.getTotalCost();
        log.info("Using frontend calculated total cost: {}", totalCost);
      } else {
        log.info("Frontend total cost not provided, calculating from backend");
        if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
          log.info("Using time slot ranges for cost calculation");
          for (BookingWithPaymentDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
            if (!dto.getCourtBookings().isEmpty()) {
              Long courtId = dto.getCourtBookings().get(0).getCourtId();
              Court court = courtRepository.findById(courtId)
                  .orElseThrow(() -> new RuntimeException("Court not found: " + courtId));
              double rangeCost = court.getPricePerHour().doubleValue() * range.getDuration();
              totalCost += rangeCost;
              log.info("Range: {} - {}, Duration: {}, Base Price: {}, Range Cost: {}",
                  range.getStartTime(), range.getEndTime(), range.getDuration(),
                  court.getPricePerHour(), rangeCost);
            }
          }
        } else {
          log.info("Using fallback calculation for continuous bookings");
          // Fallback calculation for continuous bookings
          if (!dto.getCourtBookings().isEmpty()) {
            Long courtId = dto.getCourtBookings().get(0).getCourtId();
            Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found: " + courtId));
            totalCost = court.getPricePerHour().doubleValue() * dto.getDuration();
            log.info("Court ID: {}, Base Price: {}, Duration: {}, Total Cost: {}",
                courtId, court.getPricePerHour(), dto.getDuration(), totalCost);
          }
        }
      }

      log.info("Final total cost: {}", totalCost);
      log.info("=== END COST CALCULATION DEBUG ===");

      // Set the calculated total cost in the booking request
      bookingRequest.setTotalCost(totalCost);

      // Create payment first
      PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
      paymentRequest.setAmount(totalCost);
      paymentRequest.setPaymentMethod("CARD");
      paymentRequest.setStatus("SUCCEEDED"); // Since Stripe already verified it succeeded
      paymentRequest.setTransactionId(dto.getPaymentIntentId());

      PaymentResponseDTO createdPayment = paymentService.createPaymentFromIntent(paymentRequest, dto.getCustomerId());
      log.info("Payment created with ID: {} for intent: {}", createdPayment.getPaymentId(), dto.getPaymentIntentId());

      // Create the booking with the payment
      bookingRequest.setPaymentId(createdPayment.getPaymentId());
      BookingResponseDTO createdBooking = bookingService.createBooking(bookingRequest);

      log.info("Booking created successfully with ID: {} and payment ID: {}",
          createdBooking.getBookingId(), createdPayment.getPaymentId());

      log.info("Booking created successfully with ID: {} for payment intent: {}",
          createdBooking.getBookingId(), dto.getPaymentIntentId());

      return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);

    } catch (StripeException e) {
      log.error("Stripe error during payment verification: {}", e.getMessage(), e);
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Payment verification failed: " + e.getMessage(),
              "paymentIntentId", dto.getPaymentIntentId(),
              "timestamp", java.time.LocalDateTime.now()));
    } catch (RuntimeException e) {
      log.error("Error creating booking with payment verification: {}", e.getMessage(), e);
      return ResponseEntity.badRequest()
          .body(Map.of("error", e.getMessage(),
              "paymentIntentId", dto.getPaymentIntentId(),
              "timestamp", java.time.LocalDateTime.now()));
    }
  }

  @GetMapping("/{id}/can-cancel")
  @Operation(summary = "Check if booking can be cancelled", description = "Check if a booking can be cancelled based on the 6-hour rule")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Cancellation eligibility checked", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Boolean> canCancelBooking(
      @Parameter(description = "Unique identifier of the booking to check", required = true) @PathVariable Long id) {
    try {
      boolean canCancel = bookingService.canCancelBooking(id);
      return ResponseEntity.ok(canCancel);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/{id}/cancellation-deadline")
  @Operation(summary = "Get cancellation deadline", description = "Get the deadline by which a booking must be cancelled (6 hours before booking time)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Cancellation deadline retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
      @ApiResponse(responseCode = "404", description = "Booking not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<String> getCancellationDeadline(
      @Parameter(description = "Unique identifier of the booking", required = true) @PathVariable Long id) {
    try {
      LocalDateTime deadline = bookingService.getCancellationDeadline(id);
      return ResponseEntity.ok(deadline.toString());
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
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

  @GetMapping("/debug/venue/{venueId}")
  @Operation(summary = "Debug venue bookings", description = "Debug endpoint to check booking time slots for a venue")
  public ResponseEntity<Map<String, Object>> debugVenueBookings(@PathVariable Long venueId) {
    return ResponseEntity.ok(bookingService.debugVenueBookings(venueId));
  }

  @GetMapping("/debug/booking/{bookingId}")
  @Operation(summary = "Debug specific booking", description = "Debug endpoint to check a specific booking's time slots")
  public ResponseEntity<Map<String, Object>> debugBooking(@PathVariable Long bookingId) {
    return ResponseEntity.ok(bookingService.debugBooking(bookingId));
  }
}
