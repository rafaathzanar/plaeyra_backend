package com.zanar.playera.mapper;

import com.zanar.playera.dto.BookingResponseDTO;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.BookingCourt;
import com.zanar.playera.entity.BookingEquipment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingMapper {
  public static BookingResponseDTO toBookingResponseDTO(Booking booking) {
    BookingResponseDTO dto = new BookingResponseDTO();
    dto.setBookingId(booking.getBookingId());
    dto.setBookingDate(booking.getBookingDate());
    dto.setDuration(booking.getDuration());
    dto.setTotalCost(booking.getTotalCost());
    dto.setBookingStatus(booking.getBookingStatus());
    if (booking.getCustomer() != null) {
      dto.setCustomerId(booking.getCustomer().getUserId());
    }
    if (booking.getPayment() != null) {
      dto.setPaymentId(booking.getPayment().getPaymentId());
    }
    if (booking.getBookingCourts() != null) {
      List<BookingResponseDTO.CourtBookingDTO> courtDTOs = booking.getBookingCourts().stream().map(bc -> {
        BookingResponseDTO.CourtBookingDTO cbdto = new BookingResponseDTO.CourtBookingDTO();
        cbdto.setCourtId(bc.getCourt().getCourtId());
        cbdto.setCourtName(bc.getCourt().getCourtName());
        cbdto.setTimeDuration(bc.getTimeDuration());
        return cbdto;
      }).collect(Collectors.toList());
      dto.setCourtBookings(courtDTOs);
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
        eqdto.setDepositAmount(be.getDepositAmount());
        eqdto.setRentalStatus(be.getStatus().name());
        eqdto.setReturnDate(be.getReturnDate());
        eqdto.setReturnNotes(be.getReturnNotes());
        eqdto.setIsReturned(be.isReturned());
        return eqdto;
      }).collect(Collectors.toList());
      dto.setEquipmentBookings(eqDTOs);
    }
    return dto;
  }
}