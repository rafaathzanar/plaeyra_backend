package com.zanar.playera.service;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.*;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
    private SlotRepository slotRepository;
    
    @Autowired
    private PaymentService paymentService;

    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        // Validate customer exists
        Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Validate booking date and time
        if (!dto.isValidTimeRange()) {
            throw new RuntimeException("Invalid time range: start time must be before end time");
        }
        
        if (dto.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Booking date cannot be in the past");
        }
        
        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBookingDate(dto.getBookingDateTime());
        booking.setDuration(dto.getDurationInHours());
        booking.setBookingStatus("PENDING");
        booking.setTotalCost(0.0);
        
        // Process court bookings with slot validation
        List<BookingCourt> bookingCourts = new ArrayList<>();
        double totalCourtCost = 0.0;
        
        if (dto.getCourtBookings() != null && !dto.getCourtBookings().isEmpty()) {
            for (BookingRequestDTO.CourtBookingDTO courtBooking : dto.getCourtBookings()) {
                Court court = courtRepository.findById(courtBooking.getCourtId())
                        .orElseThrow(() -> new RuntimeException("Court not found: " + courtBooking.getCourtId()));
                
                // Check for conflicting slots
                List<Slot> conflictingSlots = slotRepository.findConflictingSlots(
                        court.getCourtId(), 
                        dto.getBookingDate(), 
                        dto.getStartTime(), 
                        dto.getEndTime()
                );
                
                if (!conflictingSlots.isEmpty()) {
                    throw new RuntimeException("Court " + court.getCourtName() + " is not available for the selected time slot");
                }
                
                // Create booking court record
                BookingCourt bookingCourt = new BookingCourt();
                bookingCourt.setBooking(booking);
                bookingCourt.setCourt(court);
                bookingCourt.setTimeDuration(courtBooking.getTimeDuration());
                bookingCourts.add(bookingCourt);
                
                totalCourtCost += court.getPricePerHour() * courtBooking.getTimeDuration();
            }
        }
        
        booking.setBookingCourts(bookingCourts);
        
        // Process equipment bookings
        List<BookingEquipment> bookingEquipments = new ArrayList<>();
        double totalEquipmentCost = 0.0;
        
        if (dto.getEquipmentBookings() != null && !dto.getEquipmentBookings().isEmpty()) {
            for (BookingRequestDTO.EquipmentBookingDTO equipmentBooking : dto.getEquipmentBookings()) {
                Equipment equipment = equipmentRepository.findById(equipmentBooking.getEquipmentId())
                        .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentBooking.getEquipmentId()));
                
                // Validate equipment availability and rental requirements
                if (!equipment.canRent(equipmentBooking.getQuantity(), equipmentBooking.getTimeDuration())) {
                    throw new RuntimeException("Equipment " + equipment.getName() + " is not available for the requested rental");
                }
                
                // Calculate costs
                double unitPrice = equipment.getRatePerHour();
                double totalPrice = equipment.calculateRentalCost(equipmentBooking.getQuantity(), equipmentBooking.getTimeDuration());
                double depositAmount = equipment.calculateDeposit(equipmentBooking.getQuantity());
                
                // Create booking equipment record
                BookingEquipment bookingEquipment = new BookingEquipment();
                bookingEquipment.setBooking(booking);
                bookingEquipment.setEquipment(equipment);
                bookingEquipment.setQuantity(equipmentBooking.getQuantity());
                bookingEquipment.setTimeDuration(equipmentBooking.getTimeDuration());
                bookingEquipment.setUnitPrice(unitPrice);
                bookingEquipment.setTotalPrice(totalPrice);
                bookingEquipment.setDepositAmount(depositAmount);
                bookingEquipment.setStatus(BookingEquipment.RentalStatus.RENTED);
                bookingEquipments.add(bookingEquipment);
                
                totalEquipmentCost += totalPrice;
                
                // Reserve equipment (reduce available quantity)
                equipment.reserve(equipmentBooking.getQuantity());
                equipmentRepository.save(equipment);
            }
        }
        
        booking.setBookingEquipments(bookingEquipments);
        booking.setTotalCost(totalCourtCost + totalEquipmentCost);
        
        // Save booking and related entities
        Booking savedBooking = bookingRepository.save(booking);
        bookingCourtRepository.saveAll(bookingCourts);
        bookingEquipmentRepository.saveAll(bookingEquipments);
        
        return BookingMapper.toBookingResponseDTO(savedBooking);
    }

    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return BookingMapper.toBookingResponseDTO(booking);
    }

    public List<BookingResponseDTO> listBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingMapper::toBookingResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> listBookingsByCustomer(Long customerId) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
                .map(BookingMapper::toBookingResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if ("CANCELLED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }
        
        // Release all slots associated with this booking
        List<Slot> bookedSlots = slotRepository.findByBookingId(id);
        for (Slot slot : bookedSlots) {
            slot.release();
            slotRepository.save(slot);
        }
        
        // Restore equipment quantities and process refunds
        if (booking.getBookingEquipments() != null) {
            for (BookingEquipment bookingEquipment : booking.getBookingEquipments()) {
                Equipment equipment = bookingEquipment.getEquipment();
                
                // Release equipment back to available inventory
                equipment.release(bookingEquipment.getQuantity());
                equipmentRepository.save(equipment);
                
                // Mark equipment as returned for refund processing
                bookingEquipment.markAsReturned();
                bookingEquipmentRepository.save(bookingEquipment);
                
                // TODO: Process refund based on venue-specific policies
                // This would integrate with PaymentService for actual refund processing
                // Refund amount could be calculated based on cancellation time and policies
            }
        }
        
        // Update booking status
        booking.setBookingStatus("CANCELLED");
        bookingRepository.save(booking);
    }
    
    // New methods for real-time availability
    
    public List<Slot> getAvailableSlotsByCourt(Long courtId, LocalDate date) {
        return slotRepository.findByCourtIdAndDateAndStatus(courtId, date, Slot.SlotStatus.AVAILABLE);
    }
    
    public List<Slot> getAvailableSlotsByVenue(Long venueId, LocalDate date) {
        return slotRepository.findAvailableSlotsByVenueAndDate(venueId, date, Slot.SlotStatus.AVAILABLE);
    }
    
    public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Slot> conflictingSlots = slotRepository.findConflictingSlots(courtId, date, startTime, endTime);
        return conflictingSlots.isEmpty();
    }
    
    public List<Slot> getCourtCalendar(Long courtId, LocalDate startDate, LocalDate endDate) {
        return slotRepository.findByCourtIdAndDateBetween(courtId, startDate, endDate);
    }
    
    public void cleanupExpiredBookings() {
        List<Slot> expiredSlots = slotRepository.findExpiredBookedSlots(LocalDate.now());
        for (Slot slot : expiredSlots) {
            slot.release();
            slotRepository.save(slot);
        }
    }
}
