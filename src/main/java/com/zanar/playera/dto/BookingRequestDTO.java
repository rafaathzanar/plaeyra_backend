package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingRequestDTO {
  private Long customerId;
  private LocalDateTime bookingDate;
  private int duration;
  private List<CourtBookingDTO> courtBookings;
  private List<EquipmentBookingDTO> equipmentBookings;

  @Data
  public static class CourtBookingDTO {
    private Long courtId;
    private int timeDuration;
  }

  @Data
  public static class EquipmentBookingDTO {
    private Long equipmentId;
    private int quantity;
    private int timeDuration;
  }
}