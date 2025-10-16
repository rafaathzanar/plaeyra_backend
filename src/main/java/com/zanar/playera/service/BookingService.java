package com.zanar.playera.service;

import com.zanar.playera.dto.BookingRequestDTO;
import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.*;
import com.zanar.playera.mapper.BookingMapper;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingCourtRepository bookingCourtRepository;

    @Autowired
    private BookingEquipmentRepository bookingEquipmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingTimeSlotRepository bookingTimeSlotRepository;

    @Autowired
    private BookingValidationService bookingValidationService;

    @Autowired
    private BookingMonitoringService monitoringService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        try {
            // Comprehensive validation using the new validation service
            bookingValidationService.validateBookingRequest(dto);

            // Validate customer exists
            Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

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

            booking.setBookingStatus(Booking.BookingStatus.BOOKED); // Set to BOOKED for successful bookings

            // Use totalCost from frontend if available, otherwise calculate it
            if (dto.getTotalCost() != null && dto.getTotalCost() > 0) {
                booking.setTotalCost(dto.getTotalCost());
                log.info("Using frontend calculated total cost for booking: {}", dto.getTotalCost());
            } else {
                booking.setTotalCost(0.0); // Will be calculated later
                log.info("Frontend total cost not provided, will calculate from backend");
            }
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
                    BigDecimal totalPrice = BigDecimal
                            .valueOf(equipment.calculateRentalCost(equipmentBooking.getQuantity(),
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

            // Calculate total cost from time slot ranges if available, otherwise use court
            // booking duration
            // Only recalculate total cost if not provided by frontend
            if (booking.getTotalCost() == 0.0) {
                log.info("Recalculating total cost from backend (frontend cost not provided)");
                // Calculate total cost from time slot ranges if available, otherwise use court
                // booking duration
                BigDecimal finalTotalCost;
                if (dto.getTimeSlotRanges() != null && !dto.getTimeSlotRanges().isEmpty()) {
                    // Use time slot ranges for accurate cost calculation
                    BigDecimal timeSlotCost = BigDecimal.ZERO;
                    for (BookingRequestDTO.TimeSlotRangeDTO range : dto.getTimeSlotRanges()) {
                        if (!dto.getCourtBookings().isEmpty()) {
                            Long courtId = dto.getCourtBookings().get(0).getCourtId();
                            Court court = courtRepository.findById(courtId)
                                    .orElseThrow(() -> new RuntimeException("Court not found: " + courtId));
                            timeSlotCost = timeSlotCost
                                    .add(court.getPricePerHour().multiply(BigDecimal.valueOf(range.getDuration())));
                        }
                    }
                    finalTotalCost = timeSlotCost.add(totalEquipmentCost);
                } else {
                    // Fallback to original calculation for continuous bookings
                    finalTotalCost = totalCourtCost.add(totalEquipmentCost);
                }
                booking.setTotalCost(finalTotalCost.doubleValue());
                log.info("Backend calculated total cost: {}", finalTotalCost.doubleValue());
            } else {
                log.info("Using frontend provided total cost: {}", booking.getTotalCost());
            }

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

            // Link payment if provided
            if (dto.getPaymentId() != null) {
                Payment payment = paymentRepository.findById(dto.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + dto.getPaymentId()));
                booking.setPayment(payment);
                log.info("Linked payment ID {} to booking", dto.getPaymentId());
            }

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

                // Create notification for booking confirmation
                notificationService.createBookingConfirmationNotification(bookingWithDetails);

                return BookingMapper.toBookingResponseDTO(bookingWithDetails);
            }

            // Create notification for booking confirmation
            notificationService.createBookingConfirmationNotification(savedBooking);

            // Track successful booking
            monitoringService.trackBookingAttempt(true, false);

            return BookingMapper.toBookingResponseDTO(savedBooking);

        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);

            // Track failed booking attempt
            boolean hadConflict = e.getMessage().contains("conflict") || e.getMessage().contains("not available");
            monitoringService.trackBookingAttempt(false, hadConflict);

            // Release any locks that might have been acquired
            if (dto.getCourtBookings() != null) {
                for (BookingRequestDTO.CourtBookingDTO courtBooking : dto.getCourtBookings()) {
                    bookingValidationService.releaseCourtLock(courtBooking.getCourtId());
                }
            }

            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
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

    /**
     * Get cancellation reason based on timing
     */
    private String getCancellationReason(LocalDateTime bookingDateTime, LocalDateTime currentTime) {
        long hoursUntilBooking = java.time.Duration.between(currentTime, bookingDateTime).toHours();

        if (hoursUntilBooking >= 24) {
            return "Cancelled 24+ hours before booking";
        } else if (hoursUntilBooking >= 6) {
            return "Cancelled 6-24 hours before booking";
        } else {
            return "Cancelled less than 6 hours before booking";
        }
    }

    /**
     * Check if a booking can be cancelled (6 hours before booking time)
     */
    public boolean canCancelBooking(Long bookingId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if ("CANCELLED".equals(booking.getBookingStatus())) {
                return false;
            }

            LocalDateTime bookingDateTime = booking.getBookingDate();
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime sixHoursBeforeBooking = bookingDateTime.minusHours(6);

            return currentTime.isBefore(sixHoursBeforeBooking);
        } catch (Exception e) {
            log.error("Error checking if booking can be cancelled: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get cancellation deadline for a booking
     */
    public LocalDateTime getCancellationDeadline(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        return booking.getBookingDate().minusHours(6);
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if ("CANCELLED".equals(booking.getBookingStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }

        // Check if cancellation is allowed (6 hours before booking time)
        LocalDateTime bookingDateTime = booking.getBookingDate();
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime sixHoursBeforeBooking = bookingDateTime.minusHours(6);

        if (currentTime.isAfter(sixHoursBeforeBooking)) {
            throw new RuntimeException(
                    "Booking cannot be cancelled. Cancellation must be done at least 6 hours before the booking time.");
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
            }
        }

        // Process refund if payment exists
        if (booking.getPayment() != null) {
            try {
                // Calculate refund amount based on cancellation policy
                Double refundAmount = paymentService.calculateRefundAmount(id, "Booking cancelled by customer");

                if (refundAmount > 0) {
                    // Process refund through Stripe
                    paymentService.processRefund(
                            booking.getPayment().getPaymentId(),
                            refundAmount,
                            "Booking cancelled - " + getCancellationReason(bookingDateTime, currentTime));
                    log.info("Refund processed for booking {}: LKR {}", id, refundAmount);
                } else {
                    log.info("No refund available for booking {} - cancelled too close to booking time", id);
                }

                // Create cancellation notification with refund details
                notificationService.createBookingCancellationNotification(
                        booking,
                        refundAmount,
                        getCancellationReason(bookingDateTime, currentTime));

            } catch (Exception e) {
                log.error("Failed to process refund for booking {}: {}", id, e.getMessage());
                // Don't fail the cancellation if refund fails

                // Still create notification even if refund fails
                notificationService.createBookingCancellationNotification(
                        booking,
                        0.0,
                        "Booking cancelled - refund processing failed");
            }
        } else {
            // Create notification even if no payment exists
            notificationService.createBookingCancellationNotification(
                    booking,
                    0.0,
                    getCancellationReason(bookingDateTime, currentTime));
        }

        // Update booking status
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully. Booking time: {}, Cancellation time: {}",
                id, bookingDateTime, currentTime);
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
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED &&
                        b.getBookingCourts() != null &&
                        b.getBookingCourts().stream()
                                .anyMatch(bc -> bc.getCourt().getVenue() != null
                                        && venueIds.contains(bc.getCourt().getVenue().getVenueId())))
                .toList();
    }

    public void processRefundForCancelledBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (booking.getBookingStatus() != Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is not cancelled");
        }
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }
        // Optionally, update booking status to indicate refund processed
        booking.setBookingStatus(Booking.BookingStatus.REFUNDED);
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
