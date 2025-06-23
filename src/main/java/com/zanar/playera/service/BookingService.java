package com.zanar.playera.service;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.*;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
  @Autowired
  private BookingRepository bookingRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private CourtRepository courtRepository;
  @Autowired
  private EquipmentRepository equipmentRepository;
  @Autowired
  private BookingCourtRepository bookingCourtRepository;
  @Autowired
  private BookingEquipmentRepository bookingEquipmentRepository;
  @Autowired
  private PaymentService paymentService;

  @Transactional
  public BookingResponseDTO createBooking(BookingRequestDTO dto) {
    Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
        .orElseThrow(() -> new RuntimeException("Customer not found"));
    Booking booking = new Booking();
    booking.setCustomer(customer);
    booking.setBookingDate(dto.getBookingDate());
    booking.setDuration(dto.getDuration());
    booking.setBookingStatus("PENDING");
    booking.setTotalCost(0.0); // Will be calculated

    List<BookingCourt> bookingCourts = new ArrayList<>();
    double totalCourtCost = 0.0;
    if (dto.getCourtBookings() != null) {
      for (BookingRequestDTO.CourtBookingDTO c : dto.getCourtBookings()) {
        Court court = courtRepository.findById(c.getCourtId())
            .orElseThrow(() -> new RuntimeException("Court not found: " + c.getCourtId()));
        BookingCourt bc = new BookingCourt();
        bc.setBooking(booking);
        bc.setCourt(court);
        bc.setTimeDuration(c.getTimeDuration());
        bookingCourts.add(bc);
        totalCourtCost += court.getPricePerHour() * c.getTimeDuration();
      }
    }
    booking.setBookingCourts(bookingCourts);

    List<BookingEquipment> bookingEquipments = new ArrayList<>();
    double totalEquipmentCost = 0.0;
    if (dto.getEquipmentBookings() != null) {
      for (BookingRequestDTO.EquipmentBookingDTO e : dto.getEquipmentBookings()) {
        Equipment equipment = equipmentRepository.findById(e.getEquipmentId())
            .orElseThrow(() -> new RuntimeException("Equipment not found: " + e.getEquipmentId()));
        if (equipment.getAvailableQuantity() < e.getQuantity()) {
          throw new RuntimeException("Not enough equipment available: " + equipment.getName());
        }
        BookingEquipment be = new BookingEquipment();
        be.setBooking(booking);
        be.setEquipment(equipment);
        be.setQuantity(e.getQuantity());
        be.setTimeDuration(e.getTimeDuration());
        bookingEquipments.add(be);
        totalEquipmentCost += equipment.getRatePerHour() * e.getQuantity() * e.getTimeDuration();
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() - e.getQuantity());
        equipmentRepository.save(equipment);
      }
    }
    booking.setBookingEquipments(bookingEquipments);

    booking.setTotalCost(totalCourtCost + totalEquipmentCost);
    Booking saved = bookingRepository.save(booking);
    bookingCourtRepository.saveAll(bookingCourts);
    bookingEquipmentRepository.saveAll(bookingEquipments);
    return BookingMapper.toBookingResponseDTO(saved);
  }

  public BookingResponseDTO getBookingById(Long id) {
    Booking booking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Booking not found"));
    return BookingMapper.toBookingResponseDTO(booking);
  }

  public List<BookingResponseDTO> listBookings() {
    return bookingRepository.findAll().stream().map(BookingMapper::toBookingResponseDTO).collect(Collectors.toList());
  }

  public List<BookingResponseDTO> listBookingsByCustomer(Long customerId) {
    return bookingRepository.findAll().stream()
        .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
        .map(BookingMapper::toBookingResponseDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public void cancelBooking(Long id) {
    Booking booking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Booking not found"));
    booking.setBookingStatus("CANCELLED");
    bookingRepository.save(booking);
    // Optionally, restore equipment quantities
    if (booking.getBookingEquipments() != null) {
      for (BookingEquipment be : booking.getBookingEquipments()) {
        Equipment equipment = be.getEquipment();
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() + be.getQuantity());
        equipmentRepository.save(equipment);
      }
    }
  }
}
