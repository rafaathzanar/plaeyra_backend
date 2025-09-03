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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
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

    @Autowired
    private BookingTimeSlotRepository bookingTimeSlotRepository;

    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        // Validate customer exists
        Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Validate booking date
        if (dto.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Booking date cannot be in the past");
        }

        // Validate time slot ranges if provided (for discontinuous slots)
        if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
            for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
                if (!range.isValidTimeRange()) {
                    throw new RuntimeException("Invalid time range: start time must be before end time for range " +
                            range.getStartTime() + " - " + range.getEndTime());
                }
            }
            // For discontinuous bookings, skip the overall startTime/endTime validation
            // as they represent the span from first to last slot, not a continuous range
        } else if (!dto.isValidTimeRange()) {
            // Fallback to single time range validation for continuous bookings
            throw new RuntimeException("Invalid time range: start time must be before end time");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBookingDate(dto.getBookingDateTime());

        // Calculate duration correctly for discontinuous bookings
        double totalDuration = 0;
        if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
            // Sum up all individual time ranges for discontinuous bookings
            totalDuration = dto.getTimeSlotRanges().stream()
                    .mapToDouble(BookingRequestDTO.TimeSlotRangeDTO::getDuration)
                    .sum();
        } else {
            // Use the original duration calculation for continuous bookings
            totalDuration = dto.getDurationInHours();
        }
        booking.setDuration((int) Math.round(totalDuration));

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

        // Process time slot ranges (for discontinuous slots)
        List<BookingTimeSlot> bookingTimeSlots = new ArrayList<>();
        if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
            for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
                // Get the court from the first court booking
                if (!dto.getCourtBookings().isEmpty()) {
                    Long courtId = dto.getCourtBookings().get(0).getCourtId();
                    Court court = courtRepository.findById(courtId)
                            .orElseThrow(() -> new RuntimeException("Court not found: " + courtId));

                    // Check for conflicting time slots
                    List<BookingTimeSlot> conflictingSlots = bookingTimeSlotRepository.findConflictingTimeSlots(
                            courtId, dto.getBookingDate(), range.getStartTime(), range.getEndTime());

                    if (!conflictingSlots.isEmpty()) {
                        throw new RuntimeException("Court " + court.getCourtName() +
                                " is not available for the time range " + range.getStartTime() + " - "
                                + range.getEndTime());
                    }

                    // Calculate cost for this time range
                    double rangeCost = court.getPricePerHour().doubleValue() * range.getDuration();

                    // Create booking time slot
                    BookingTimeSlot bookingTimeSlot = new BookingTimeSlot();
                    bookingTimeSlot.setBooking(booking);
                    bookingTimeSlot.setCourt(court);
                    bookingTimeSlot.setStartTime(range.getStartTime());
                    bookingTimeSlot.setEndTime(range.getEndTime());
                    bookingTimeSlot.setDuration(range.getDuration());
                    bookingTimeSlot.setCost(rangeCost);
                    bookingTimeSlots.add(bookingTimeSlot);
                }
            }
        }

        booking.setBookingTimeSlots(bookingTimeSlots);

        // Save booking and related entities
        Booking savedBooking = bookingRepository.save(booking);
        log.info("=== BOOKING SAVE DEBUG ===");
        log.info("Saved booking ID: {}", savedBooking.getBookingId());
        log.info("BookingTimeSlots to save: {}", bookingTimeSlots.size());

        // Set the booking reference for all time slots after booking is saved
        for (BookingTimeSlot bookingTimeSlot : bookingTimeSlots) {
            bookingTimeSlot.setBooking(savedBooking);
            log.info("BookingTimeSlot: {} - {} (duration: {})",
                    bookingTimeSlot.getStartTime(),
                    bookingTimeSlot.getEndTime(),
                    bookingTimeSlot.getDuration());
        }

        bookingCourtRepository.saveAll(bookingCourts);
        bookingEquipmentRepository.saveAll(bookingEquipments);
        List<BookingTimeSlot> savedTimeSlots = bookingTimeSlotRepository.saveAll(bookingTimeSlots);
        log.info("Saved BookingTimeSlots count: {}", savedTimeSlots.size());
        log.info("=== END BOOKING SAVE DEBUG ===");

        // Update slot status to BOOKED for the booked time slots
        updateSlotsToBooked(savedBooking, dto);

        // Load the booking with all relationships for proper DTO mapping
        Booking bookingWithDetails = bookingRepository.findById(savedBooking.getBookingId()).orElse(null);
        if (bookingWithDetails != null) {
            // Load time slot relationships
            Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(savedBooking.getBookingId());
            if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
                bookingWithDetails.setBookingTimeSlots(bookingWithTimeSlots.getBookingTimeSlots());
            }

            // Load equipment relationships
            Booking bookingWithEquipment = bookingRepository.findByIdWithEquipment(savedBooking.getBookingId());
            if (bookingWithEquipment != null && bookingWithEquipment.getBookingEquipments() != null) {
                bookingWithDetails.setBookingEquipments(bookingWithEquipment.getBookingEquipments());
            }

            return BookingMapper.toBookingResponseDTO(bookingWithDetails);
        }

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

                // Handle discontinuous time slots if provided
                if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
                    for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
                        updateSlotsForTimeRange(courtId, bookingDate, range.getStartTime(), range.getEndTime(),
                                booking);
                    }
                } else {
                    // Fallback to single time range
                    LocalTime startTime = dto.getStartTime();
                    LocalTime endTime = dto.getEndTime();
                    updateSlotsForTimeRange(courtId, bookingDate, startTime, endTime, booking);
                }
            }
        } catch (Exception e) {
            log.error("Error updating slots to BOOKED status for booking {}: {}", booking.getBookingId(),
                    e.getMessage());
            // Don't throw exception here as booking is already created
        }
    }

    /**
     * Update slots for a specific time range
     */
    private void updateSlotsForTimeRange(Long courtId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
            Booking booking) {
        // Find and update slots for the booked time range
        List<Slot> allSlots = slotRepository.findByCourt_CourtIdAndDate(courtId, bookingDate);

        // Filter slots that overlap with the booked time range
        List<Slot> slotsToUpdate = allSlots.stream()
                .filter(slot -> slot.getStartTime().isBefore(endTime) &&
                        slot.getEndTime().isAfter(startTime))
                .collect(Collectors.toList());

        // Update slots to BOOKED status
        for (Slot slot : slotsToUpdate) {
            slot.setStatus(Slot.SlotStatus.BOOKED);
            slot.setBooking(booking);
            slotRepository.save(slot);
        }
        log.info("Updated {} slots to BOOKED status for time range {} - {} in booking {}",
                slotsToUpdate.size(), startTime, endTime, booking.getBookingId());
    }

    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return BookingMapper.toBookingResponseDTO(booking);
    }

    public List<BookingResponseDTO> listBookings() {
        List<Booking> bookings = bookingRepository.findAll();

        // Load missing relationships to avoid MultipleBagFetchException
        for (Booking booking : bookings) {
            // Load equipment relationships
            Booking bookingWithEquipment = bookingRepository.findByIdWithEquipment(booking.getBookingId());
            if (bookingWithEquipment != null && bookingWithEquipment.getBookingEquipments() != null) {
                booking.setBookingEquipments(bookingWithEquipment.getBookingEquipments());
            }

            // Load time slot relationships
            Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(booking.getBookingId());
            if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
                booking.setBookingTimeSlots(bookingWithTimeSlots.getBookingTimeSlots());
            }
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> listBookingsByCustomer(Long customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(customerId);

        // Load missing relationships to avoid MultipleBagFetchException
        for (Booking booking : bookings) {
            // Load equipment relationships
            Booking bookingWithEquipment = bookingRepository.findByIdWithEquipment(booking.getBookingId());
            if (bookingWithEquipment != null && bookingWithEquipment.getBookingEquipments() != null) {
                booking.setBookingEquipments(bookingWithEquipment.getBookingEquipments());
            }

            // Load time slot relationships
            Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(booking.getBookingId());
            if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
                booking.setBookingTimeSlots(bookingWithTimeSlots.getBookingTimeSlots());
            }
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> listBookingsByVenue(Long venueId) {
        List<Booking> bookings = bookingRepository.findByVenueIdWithDetails(venueId);

        // Load missing relationships to avoid MultipleBagFetchException
        for (Booking booking : bookings) {
            // Load equipment relationships
            Booking bookingWithEquipment = bookingRepository.findByIdWithEquipment(booking.getBookingId());
            if (bookingWithEquipment != null && bookingWithEquipment.getBookingEquipments() != null) {
                booking.setBookingEquipments(bookingWithEquipment.getBookingEquipments());
            }

            // Load time slot relationships
            Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(booking.getBookingId());
            if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
                booking.setBookingTimeSlots(bookingWithTimeSlots.getBookingTimeSlots());
                log.info("=== VENUE BOOKING DEBUG ===");
                log.info("Booking ID: {}", booking.getBookingId());
                log.info("BookingTimeSlots loaded: {}", bookingWithTimeSlots.getBookingTimeSlots().size());
                log.info("TimeSlotRanges will be set: {}", !bookingWithTimeSlots.getBookingTimeSlots().isEmpty());
                log.info("=== END VENUE BOOKING DEBUG ===");
            } else {
                log.info("=== VENUE BOOKING DEBUG ===");
                log.info("Booking ID: {}", booking.getBookingId());
                log.info("No BookingTimeSlots found for this booking");
                log.info("=== END VENUE BOOKING DEBUG ===");
            }
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> debugVenueBookings(Long venueId) {
        List<Booking> bookings = bookingRepository.findByVenueIdWithDetails(venueId);

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("totalBookings", bookings.size());

        List<Map<String, Object>> bookingDetails = new ArrayList<>();
        for (Booking booking : bookings) {
            Map<String, Object> bookingInfo = new HashMap<>();
            bookingInfo.put("bookingId", booking.getBookingId());
            bookingInfo.put("bookingDate", booking.getBookingDate());
            bookingInfo.put("startTime", booking.getBookingDate().toLocalTime());
            bookingInfo.put("duration", booking.getDuration());

            // Check if BookingTimeSlots exist
            Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(booking.getBookingId());
            if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
                bookingInfo.put("hasTimeSlots", true);
                bookingInfo.put("timeSlotCount", bookingWithTimeSlots.getBookingTimeSlots().size());
                List<Map<String, Object>> timeSlots = new ArrayList<>();
                for (var bts : bookingWithTimeSlots.getBookingTimeSlots()) {
                    Map<String, Object> ts = new HashMap<>();
                    ts.put("startTime", bts.getStartTime());
                    ts.put("endTime", bts.getEndTime());
                    ts.put("duration", bts.getDuration());
                    timeSlots.add(ts);
                }
                bookingInfo.put("timeSlots", timeSlots);
            } else {
                bookingInfo.put("hasTimeSlots", false);
                bookingInfo.put("timeSlotCount", 0);
            }

            bookingDetails.add(bookingInfo);
        }

        debugInfo.put("bookings", bookingDetails);
        return debugInfo;
    }

    public Map<String, Object> debugBooking(Long bookingId) {
        Map<String, Object> debugInfo = new HashMap<>();

        // Get the booking
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            debugInfo.put("error", "Booking not found");
            return debugInfo;
        }

        debugInfo.put("bookingId", booking.getBookingId());
        debugInfo.put("bookingDate", booking.getBookingDate());
        debugInfo.put("duration", booking.getDuration());

        // Check if BookingTimeSlots exist
        Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(bookingId);
        if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
            debugInfo.put("hasTimeSlots", true);
            debugInfo.put("timeSlotCount", bookingWithTimeSlots.getBookingTimeSlots().size());
            List<Map<String, Object>> timeSlots = new ArrayList<>();
            for (var bts : bookingWithTimeSlots.getBookingTimeSlots()) {
                Map<String, Object> ts = new HashMap<>();
                ts.put("id", bts.getId());
                ts.put("startTime", bts.getStartTime());
                ts.put("endTime", bts.getEndTime());
                ts.put("duration", bts.getDuration());
                ts.put("cost", bts.getCost());
                timeSlots.add(ts);
            }
            debugInfo.put("timeSlots", timeSlots);
        } else {
            debugInfo.put("hasTimeSlots", false);
            debugInfo.put("timeSlotCount", 0);
        }

        return debugInfo;
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
