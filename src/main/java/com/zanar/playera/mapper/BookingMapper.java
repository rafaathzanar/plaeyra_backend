package com.zanar.playera.mapper;

import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.BookingCourt;
import com.zanar.playera.entity.BookingEquipment;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {
  public static BookingResponseDTO toBookingResponseDTO(Booking booking) {
    BookingResponseDTO dto = new BookingResponseDTO();
    dto.setBookingId(booking.getBookingId());
    dto.setBookingDate(booking.getBookingDate());
    dto.setCreatedAt(booking.getCreatedAt());
    dto.setDate(booking.getBookingDate().toLocalDate()); // For backward compatibility
    dto.setDuration(booking.getDuration());
    dto.setTotalCost(booking.getTotalCost());
    dto.setTotalAmount(booking.getTotalCost()); // For backward compatibility
    dto.setBookingStatus(booking.getBookingStatus().name());
    dto.setStatus(booking.getBookingStatus().name()); // For backward compatibility
    dto.setSpecialRequests(booking.getSpecialRequests());
    dto.setNotes(booking.getSpecialRequests()); // For backward compatibility

    // Customer information
    if (booking.getCustomer() != null) {
      dto.setCustomerId(booking.getCustomer().getUserId());
      dto.setCustomerName(booking.getCustomer().getName());
      dto.setCustomerEmail(booking.getCustomer().getEmail());
      dto.setCustomerPhone(booking.getCustomer().getPhone());
    }

    if (booking.getPayment() != null) {
      dto.setPaymentId(booking.getPayment().getPaymentId());
      dto.setPaymentStatus(booking.getPayment().getStatus().toString());
      dto.setPaymentMethod(booking.getPayment().getPaymentMethod().toString());
      dto.setPaymentAmount(booking.getPayment().getAmount());
      dto.setStripePaymentIntentId(booking.getPayment().getStripePaymentIntentId());
    }
    if (booking.getBookingCourts() != null && !booking.getBookingCourts().isEmpty()) {
      List<BookingResponseDTO.CourtBookingDTO> courtDTOs = booking.getBookingCourts().stream().map(bc -> {
        BookingResponseDTO.CourtBookingDTO cbdto = new BookingResponseDTO.CourtBookingDTO();
        cbdto.setCourtId(bc.getCourt().getCourtId());
        cbdto.setCourtName(bc.getCourt().getCourtName());
        cbdto.setCourtType(bc.getCourt().getType() != null ? bc.getCourt().getType().name() : "UNKNOWN");
        cbdto.setTimeDuration(bc.getTimeDuration());
        return cbdto;
      }).collect(Collectors.toList());
      dto.setCourtBookings(courtDTOs);

      // Set venue and court information from the first court booking
      BookingCourt firstCourt = booking.getBookingCourts().get(0);
      if (firstCourt.getCourt() != null) {
        dto.setCourtId(firstCourt.getCourt().getCourtId());
        dto.setCourtName(firstCourt.getCourt().getCourtName());
        if (firstCourt.getCourt().getVenue() != null) {
          dto.setVenueId(firstCourt.getCourt().getVenue().getVenueId());
          dto.setVenueName(firstCourt.getCourt().getVenue().getName());
        }
      }

      // Extract start and end times from booking date
      // For discontinuous bookings, use the first time slot's start time and last
      // time slot's end time
      if (booking.getBookingTimeSlots() != null && !booking.getBookingTimeSlots().isEmpty()) {
        // For discontinuous bookings, use actual time slot ranges
        dto.setStartTime(booking.getBookingTimeSlots().get(0).getStartTime());
        dto.setEndTime(booking.getBookingTimeSlots().get(booking.getBookingTimeSlots().size() - 1).getEndTime());
      } else {
        // For continuous bookings, calculate from booking date and duration
        dto.setStartTime(booking.getBookingDate().toLocalTime());
        dto.setEndTime(booking.getBookingDate().toLocalTime().plusHours(booking.getDuration()));
      }
    }
    if (booking.getBookingEquipments() != null) {
      List<BookingResponseDTO.EquipmentBookingDTO> eqDTOs = booking.getBookingEquipments().stream().map(be -> {
        BookingResponseDTO.EquipmentBookingDTO eqdto = new BookingResponseDTO.EquipmentBookingDTO();
        eqdto.setEquipmentId(be.getEquipment().getEquipmentId());
        eqdto.setName(be.getEquipment().getName());
        eqdto.setDescription(be.getEquipment().getDescription());
        eqdto.setQuantity(be.getQuantity());
        eqdto.setTimeDuration(be.getTimeDuration());
        eqdto.setUnitPrice(be.getUnitPrice());
        eqdto.setTotalPrice(be.getTotalPrice());

        eqdto.setRentalStatus(be.getStatus().name());
        eqdto.setReturnDate(be.getReturnDate());
        eqdto.setReturnNotes(be.getReturnNotes());
        eqdto.setIsReturned(be.isReturned());
        return eqdto;
      }).collect(Collectors.toList());
      dto.setEquipmentBookings(eqDTOs);
    }

    // Map time slot ranges for discontinuous bookings
    if (booking.getBookingTimeSlots() != null && !booking.getBookingTimeSlots().isEmpty()) {
      System.out.println("=== BOOKING MAPPER DEBUG ===");
      System.out.println("Booking ID: " + booking.getBookingId());
      System.out.println("BookingTimeSlots size: " + booking.getBookingTimeSlots().size());

      List<BookingResponseDTO.TimeSlotRangeDTO> timeSlotRanges = booking.getBookingTimeSlots().stream()
          .map(bts -> {
            BookingResponseDTO.TimeSlotRangeDTO rangeDTO = new BookingResponseDTO.TimeSlotRangeDTO();
            rangeDTO.setStartTime(bts.getStartTime());
            rangeDTO.setEndTime(bts.getEndTime());
            rangeDTO.setDuration(bts.getDuration());
            rangeDTO.setCost(bts.getCost());
            System.out.println("Mapped time slot: " + bts.getStartTime() + " - " + bts.getEndTime());
            return rangeDTO;
          })
          .collect(Collectors.toList());
      dto.setTimeSlotRanges(timeSlotRanges);
      System.out.println("TimeSlotRanges set in DTO: " + timeSlotRanges.size());
      System.out.println("=== END BOOKING MAPPER DEBUG ===");
    } else {
      System.out.println("=== BOOKING MAPPER DEBUG ===");
      System.out.println("Booking ID: " + booking.getBookingId());
      System.out.println("No BookingTimeSlots found - timeSlotRanges will be null");
      System.out.println("=== END BOOKING MAPPER DEBUG ===");
    }

    return dto;
  }
}