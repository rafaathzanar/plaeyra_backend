package com.zanar.playera.controller;

import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.dto.CustomerCalendarDTO;
import com.zanar.playera.entity.Slot;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.service.BookingService;
import com.zanar.playera.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer-calendar")
@CrossOrigin(origins = "*")
public class CustomerCalendarController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private SlotService slotService;

    @GetMapping("/all")
    public ResponseEntity<CustomerCalendarDTO> getCustomerCalendar(@RequestParam Long customerId) {
        var past = bookingService.getPastBookingsByCustomer(customerId).stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
        var upcoming = bookingService.getUpcomingBookingsByCustomer(customerId).stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
        CustomerCalendarDTO dto = new CustomerCalendarDTO();
        dto.setPastBookings(past);
        dto.setUpcomingBookings(upcoming);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/by-venue")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByVenue(@RequestParam Long customerId, @RequestParam Long venueId) {
        var bookings = bookingService.getBookingsByCustomerAndVenue(customerId, venueId).stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByDate(@RequestParam Long customerId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var bookings = bookingService.getBookingsByCustomerAndDate(customerId, date).stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<Slot>> getAvailableSlots(@RequestParam Long courtId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var slots = slotService.getAvailableSlotsByCourtAndDate(courtId, date);
        return ResponseEntity.ok(slots);
    }
} 