package com.zanar.playera.controller;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
  @Autowired
  private BookingService bookingService;

  @GetMapping
  public ResponseEntity<List<BookingResponseDTO>> listBookings() {
    return ResponseEntity.ok(bookingService.listBookings());
  }

  @GetMapping("/{id}")
  public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<BookingResponseDTO>> listBookingsByCustomer(@PathVariable Long customerId) {
    return ResponseEntity.ok(bookingService.listBookingsByCustomer(customerId));
  }

  @PostMapping
  public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO dto) {
    return ResponseEntity.ok(bookingService.createBooking(dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
    bookingService.cancelBooking(id);
    return ResponseEntity.noContent().build();
  }
}
