package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingResponseDTO {
  private Long bookingId;
  private LocalDateTime bookingDate;
  private LocalDateTime createdAt; // When the booking was created
  private LocalDate date; // For backward compatibility
  private LocalTime startTime;
  private LocalTime endTime;
  private int duration;
  private double totalCost;
  private double totalAmount; // For backward compatibility
  private String bookingStatus;
  private String status; // For backward compatibility
  private Long customerId;
  private String customerName;
  private String customerEmail;
  private String customerPhone;
  private Long paymentId;
  private String specialRequests;
  private String notes; // For backward compatibility

  // Venue information
  private Long venueId;
  private String venueName;

  // Court information (for backward compatibility)
  private Long courtId;
  private String courtName;

  private List<CourtBookingDTO> courtBookings;
  private List<EquipmentBookingDTO> equipmentBookings;

  // NEW: Time slot ranges for discontinuous bookings
  private List<TimeSlotRangeDTO> timeSlotRanges;

  @Data
  public static class CourtBookingDTO {
    private Long courtId;
    private String courtName;
    private String courtType;
    private int timeDuration;
  }

  @Data
  public static class EquipmentBookingDTO {
    private Long equipmentId;
    private String name;
    private String description;
    private int quantity;
    private int timeDuration;
    private double unitPrice;
    private double totalPrice;

    private String rentalStatus;
    private LocalDateTime returnDate;
    private String returnNotes;
    private boolean isReturned;

    // Explicit setter for isReturned to ensure it's available
    public void setIsReturned(boolean isReturned) {
      this.isReturned = isReturned;
    }
  }

  @Data
  public static class TimeSlotRangeDTO {
    private LocalTime startTime;
    private LocalTime endTime;
    private double duration; // in hours
    private double cost; // cost for this specific range
  }
}