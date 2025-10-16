package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingWithPaymentDTO {

  @NotNull(message = "Customer ID is required")
  private Long customerId;

  @NotNull(message = "Booking date is required")
  private LocalDate bookingDate;

  @NotNull(message = "Start time is required")
  private LocalTime startTime;

  @NotNull(message = "End time is required")
  private LocalTime endTime;

  @NotNull(message = "Duration is required")
  private Integer duration;

  private String specialRequests;

  @NotNull(message = "Court bookings are required")
  private List<CourtBookingDTO> courtBookings;

  private List<EquipmentBookingDTO> equipmentBookings;

  private List<TimeSlotRangeDTO> timeSlotRanges;

  // Payment verification
  @NotNull(message = "Payment intent ID is required")
  private String paymentIntentId;

  // Total cost calculated by frontend (includes dynamic pricing)
  private Double totalCost;

  @Data
  public static class CourtBookingDTO {
    @NotNull(message = "Court ID is required")
    private Long courtId;

    @NotNull(message = "Time duration is required")
    private Integer timeDuration;
  }

  @Data
  public static class EquipmentBookingDTO {
    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotNull(message = "Time duration is required")
    private Integer timeDuration;
  }

  @Data
  public static class TimeSlotRangeDTO {
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Duration is required")
    private Integer duration;

    public boolean isValidTimeRange() {
      return startTime != null && endTime != null && startTime.isBefore(endTime);
    }
  }
}
