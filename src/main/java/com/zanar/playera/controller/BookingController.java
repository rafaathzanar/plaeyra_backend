//package com.zanar.playera.controller;
//
//import com.zanar.playera.entity.Booking;
//import com.zanar.playera.service.BookingService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/bookings")
//public class BookingController {
//
//    @Autowired
//    private BookingService bookingService;
//
//    /**
//     * Create a new booking for a customer and a slot.
//     *
//     * @param customerId The ID of the customer making the booking.
//     * @param slotId The ID of the slot being booked.
//     * @param date The booking date.
//     * @return The created Booking object.
//     */
//    @PostMapping("/create")
//    public ResponseEntity<Booking> createBooking(
//            @RequestParam Long customerId,
//            @RequestParam Long slotId,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
//        Booking booking = bookingService.createBooking(customerId, slotId, date);
//        return ResponseEntity.ok(booking);
//    }
//
//    /**
//     * Retrieve all bookings for a specific customer.
//     *
//     * @param customerId The ID of the customer.
//     * @return List of bookings for the given customer.
//     */
//    @GetMapping("/customer/{customerId}")
//    public ResponseEntity<List<Booking>> getCustomerBookings(@PathVariable Long customerId) {
//        List<Booking> bookings = bookingService.getCustomerBookings(customerId);
//        return ResponseEntity.ok(bookings);
//    }
//
//    /**
//     * Retrieve a specific booking by its ID.
//     *
//     * @param bookingId The ID of the booking.
//     * @return The Booking object.
//     */
//    @GetMapping("/{bookingId}")
//    public ResponseEntity<Booking> getBookingById(@PathVariable Long bookingId) {
//        Booking booking = bookingService.getBookingById(bookingId);
//        return ResponseEntity.ok(booking);
//    }
//
//    /**
//     * Cancel an existing booking.
//     *
//     * @param bookingId The ID of the booking to be canceled.
//     * @return The updated Booking object with the status set to CANCELLED.
//     */
//    @PutMapping("/cancel/{bookingId}")
//    public ResponseEntity<Booking> cancelBooking(@PathVariable Long bookingId) {
//        Booking cancelledBooking = bookingService.cancelBooking(bookingId);
//        return ResponseEntity.ok(cancelledBooking);
//    }
//}
