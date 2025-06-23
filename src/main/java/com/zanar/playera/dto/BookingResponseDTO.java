package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponseDTO {
  private Long bookingId;
  private LocalDateTime bookingDate;
  private int duration;
  private double totalCost;
  private String bookingStatus;
  private Long customerId;
  private Long paymentId;
  private List<CourtBookingDTO> courtBookings;
  private List<EquipmentBookingDTO> equipmentBookings;

  @Data
  public static class CourtBookingDTO {
    private Long courtId;
    private String courtName;
    private int timeDuration;
  }

  @Data
  public static class EquipmentBookingDTO {
    private Long equipmentId;
    private String name;
    private int quantity;
    private int timeDuration;
  }
}