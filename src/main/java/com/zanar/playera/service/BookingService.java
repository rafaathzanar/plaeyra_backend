package com.zanar.playera.service;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.*;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
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

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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
        booking.setBookingStatus("CONFIRMED"); // Set to CONFIRMED for successful bookings
        booking.setTotalCost(0.0);
        booking.setSpecialRequests(dto.getSpecialRequests());

        // Process court bookings with slot validation
        List<BookingCourt> bookingCourts = new ArrayList<>();
        BigDecimal totalCourtCost = BigDecimal.ZERO;

        if (dto.getCourtBookings() != null && !dto.getCourtBookings().isEmpty()) {
            for (BookingRequestDTO.CourtBookingDTO courtBooking : dto.getCourtBookings()) {
                Court court = courtRepository.findById(courtBooking.getCourtId())
                        .orElseThrow(() -> new RuntimeException("Court not found: " + courtBooking.getCourtId()));

                // Check for conflicting slots
                List<Slot> conflictingSlots = slotRepository.findConflictingSlots(
                        court.getCourtId(),
                        dto.getBookingDate(),
                        dto.getStartTime(),
                        dto.getEndTime());

                if (!conflictingSlots.isEmpty()) {
                    throw new RuntimeException(
                            "Court " + court.getCourtName() + " is not available for the selected time slot");
                }

                // Create booking court record
                BookingCourt bookingCourt = new BookingCourt();
                bookingCourt.setBooking(booking);
                bookingCourt.setCourt(court);
                bookingCourt.setTimeDuration(courtBooking.getTimeDuration());
                bookingCourts.add(bookingCourt);

                totalCourtCost = totalCourtCost
                        .add(court.getPricePerHour().multiply(BigDecimal.valueOf(courtBooking.getTimeDuration())));
            }
        }

        booking.setBookingCourts(bookingCourts);

        // Process equipment bookings
        List<BookingEquipment> bookingEquipments = new ArrayList<>();
        BigDecimal totalEquipmentCost = BigDecimal.ZERO;

        if (dto.getEquipmentBookings() != null && !dto.getEquipmentBookings().isEmpty()) {
            for (BookingRequestDTO.EquipmentBookingDTO equipmentBooking : dto.getEquipmentBookings()) {
                Equipment equipment = equipmentRepository.findById(equipmentBooking.getEquipmentId())
                        .orElseThrow(() -> new RuntimeException(
                                "Equipment not found: " + equipmentBooking.getEquipmentId()));

                // Validate equipment availability and rental requirements
                if (!equipment.canRent(equipmentBooking.getQuantity(), equipmentBooking.getTimeDuration())) {
                    throw new RuntimeException(
                            "Equipment " + equipment.getName() + " is not available for the requested rental");
                }

                // Calculate costs
                BigDecimal unitPrice = BigDecimal.valueOf(equipment.getRatePerHour());
                BigDecimal totalPrice = BigDecimal.valueOf(equipment.calculateRentalCost(equipmentBooking.getQuantity(),
                        equipmentBooking.getTimeDuration()));
                BigDecimal depositAmount = BigDecimal.ZERO; // No deposit required

                // Create booking equipment record
                BookingEquipment bookingEquipment = new BookingEquipment();
                bookingEquipment.setBooking(booking);
                bookingEquipment.setEquipment(equipment);
                bookingEquipment.setQuantity(equipmentBooking.getQuantity());
                bookingEquipment.setTimeDuration(equipmentBooking.getTimeDuration());
                bookingEquipment.setUnitPrice(unitPrice.doubleValue());
                bookingEquipment.setTotalPrice(totalPrice.doubleValue());
                // No deposit required
                bookingEquipment.setStatus(BookingEquipment.RentalStatus.RENTED);
                bookingEquipments.add(bookingEquipment);

                totalEquipmentCost = totalEquipmentCost.add(totalPrice);

                // Reserve equipment (reduce available quantity)
                equipment.reserve(equipmentBooking.getQuantity());
                equipmentRepository.save(equipment);
            }
        }

        booking.setBookingEquipments(bookingEquipments);
        booking.setTotalCost(totalCourtCost.add(totalEquipmentCost).doubleValue());

        // Save booking and related entities
        Booking savedBooking = bookingRepository.save(booking);
        bookingCourtRepository.saveAll(bookingCourts);
        bookingEquipmentRepository.saveAll(bookingEquipments);

        // Update slot status to BOOKED for the booked time slots
        updateSlotsToBooked(savedBooking, dto);

        return BookingMapper.toBookingResponseDTO(savedBooking);
    }

    /**
     * Update slot status to BOOKED for the booked time slots
     */
    private void updateSlotsToBooked(Booking booking, BookingRequestDTO dto) {
        try {
            // Get the court from the first court booking
            if (dto.getCourtBookings() != null && !dto.getCourtBookings().isEmpty()) {
                Long courtId = dto.getCourtBookings().get(0).getCourtId();
                LocalDate bookingDate = dto.getBookingDate();
                LocalTime startTime = dto.getStartTime();
                LocalTime endTime = dto.getEndTime();

                // Find and update slots for the booked time range
                List<Slot> allSlots = slotRepository.findByCourt_CourtIdAndDate(courtId, bookingDate);

                // Filter slots that fall within the booked time range
                List<Slot> slotsToUpdate = allSlots.stream()
                        .filter(slot -> slot.getStartTime().isAfter(startTime.minusMinutes(1)) &&
                                slot.getEndTime().isBefore(endTime.plusMinutes(1)))
                        .collect(Collectors.toList());

                for (Slot slot : slotsToUpdate) {
                    slot.setStatus(Slot.SlotStatus.BOOKED);
                    slot.setBooking(booking);
                    slotRepository.save(slot);
                }

                log.info("Updated {} slots to BOOKED status for booking {}", slotsToUpdate.size(),
                        booking.getBookingId());
            }
        } catch (Exception e) {
            log.error("Error updating slots to BOOKED status for booking {}: {}", booking.getBookingId(),
                    e.getMessage());
            // Don't throw exception here as booking is already created
        }
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

    public List<BookingResponseDTO> listBookingsByVenue(Long venueId) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getBookingCourts() != null &&
                        b.getBookingCourts().stream()
                                .anyMatch(bc -> bc.getCourt() != null &&
                                        bc.getCourt().getVenue() != null &&
                                        bc.getCourt().getVenue().getVenueId().equals(venueId)))
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
        List<Slot> bookedSlots = slotRepository.findByBooking_BookingId(id);
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
        return slotRepository.findByCourt_CourtIdAndDateAndStatus(courtId, date, Slot.SlotStatus.AVAILABLE);
    }

    public List<Slot> getAvailableSlotsByVenue(Long venueId, LocalDate date) {
        return slotRepository.findAvailableSlotsByVenueAndDate(venueId, date, Slot.SlotStatus.AVAILABLE);
    }

    public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Slot> conflictingSlots = slotRepository.findConflictingSlots(courtId, date, startTime, endTime);
        return conflictingSlots.isEmpty();
    }

    public List<Slot> getCourtCalendar(Long courtId, LocalDate startDate, LocalDate endDate) {
        return slotRepository.findByCourt_CourtIdAndDateBetween(courtId, startDate, endDate);
    }

    public void cleanupExpiredBookings() {
        List<Slot> expiredSlots = slotRepository.findExpiredBookedSlots(LocalDate.now());
        for (Slot slot : expiredSlots) {
            slot.release();
            slotRepository.save(slot);
        }
    }

    public List<Booking> getCancelledBookingsByOwner(Long ownerId) {
        // Find all venues owned by the owner
        List<Long> venueIds = venueRepository.findAll().stream()
                .filter(v -> v.getVenueOwner() != null && v.getVenueOwner().getUserId().equals(ownerId))
                .map(Venue::getVenueId)
                .toList();
        // Find all bookings for those venues that are cancelled
        return bookingRepository.findAll().stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getBookingStatus()) &&
                        b.getBookingCourts() != null &&
                        b.getBookingCourts().stream()
                                .anyMatch(bc -> bc.getCourt().getVenue() != null
                                        && venueIds.contains(bc.getCourt().getVenue().getVenueId())))
                .toList();
    }

    public void processRefundForCancelledBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!"CANCELLED".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new RuntimeException("Booking is not cancelled");
        }
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }
        // Optionally, update booking status to indicate refund processed
        booking.setBookingStatus("REFUNDED");
        bookingRepository.save(booking);
    }

    public List<Booking> getPastBookingsByCustomer(Long customerId) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().isBefore(java.time.LocalDateTime.now()))
                .toList();
    }

    public List<Booking> getUpcomingBookingsByCustomer(Long customerId) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().isAfter(java.time.LocalDateTime.now()))
                .toList();
    }

    public List<Booking> getBookingsByCustomerAndVenue(Long customerId, Long venueId) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
                .filter(b -> b.getBookingCourts() != null && b.getBookingCourts().stream()
                        .anyMatch(bc -> bc.getCourt().getVenue() != null
                                && bc.getCourt().getVenue().getVenueId().equals(venueId)))
                .toList();
    }

    public List<Booking> getBookingsByCustomerAndDate(Long customerId, java.time.LocalDate date) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && b.getCustomer().getUserId().equals(customerId))
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().toLocalDate().equals(date))
                .toList();
    }
}
