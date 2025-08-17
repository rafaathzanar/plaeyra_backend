package com.zanar.playera.controller;

import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner-bookings")
@CrossOrigin(origins = "*")
public class OwnerBookingController {
    @Autowired
    private BookingService bookingService;

    @GetMapping("/cancelled")
    public ResponseEntity<List<BookingResponseDTO>> getCancelledBookings(@RequestParam Long ownerId) {
        var bookings = bookingService.getCancelledBookingsByOwner(ownerId)
                .stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/process-refund/{bookingId}")
    public ResponseEntity<Void> processRefund(@PathVariable Long bookingId) {
        bookingService.processRefundForCancelledBooking(bookingId);
        return ResponseEntity.ok().build();
    }
} 