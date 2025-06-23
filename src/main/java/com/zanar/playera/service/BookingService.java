//package com.zanar.playera.service;
//
//import com.zanar.playera.entity.Booking;
//import com.zanar.playera.entity.Customer;
//import com.zanar.playera.entity.Slot;
//import com.zanar.playera.repo.BookingRepository;
//import com.zanar.playera.repo.CustomerRepository;
//import com.zanar.playera.repo.SlotRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.List;
//
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class BookingService {
//    @Autowired
//    private BookingRepository bookingRepository;
//
//    @Autowired
//    private SlotRepository slotRepository;
//
//    @Autowired
//    private CustomerRepository customerRepository;
//
//    @Transactional
//    public Booking createBooking(Long customerId, Long slotId, LocalDate date) {
//        Customer customer = customerRepository.findById(customerId)
//                .orElseThrow(() -> new RuntimeException("Customer not found"));
//
//        Slot slot = slotRepository.findById(slotId)
//                .orElseThrow(() -> new RuntimeException("Slot not found"));
//
//        if (slot.getStatus() == Slot.SlotStatus.BOOKED) {
//            throw new RuntimeException("Slot already booked");
//        }
//
//        Booking booking = new Booking();
//        booking.setCustomer(customer);
//        booking.setSlot(slot);
//        booking.setCourt(slot.getCourt());
//        booking.setBookingDate(date);
//        booking.setStatus(Booking.BookingStatus.PENDING);
//
//        Booking savedBooking = bookingRepository.save(booking);
//
//        slot.setStatus(Slot.SlotStatus.BOOKED);
//        slotRepository.save(slot);
//
//        return savedBooking;
//    }
//}
//
//
