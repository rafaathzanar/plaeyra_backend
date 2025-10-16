package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingRequestDTO {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Min(value = 1, message = "Duration must be at least 1 hour")
    private int duration; // in hours

    private String specialRequests; // Optional special requests from customer

    private List<CourtBookingDTO> courtBookings;
    private List<EquipmentBookingDTO> equipmentBookings;

    // NEW: Support for multiple time slot ranges (discontinuous slots)
    private List<TimeSlotRangeDTO> timeSlotRanges;

    @Data
    public static class CourtBookingDTO {
        @NotNull(message = "Court ID is required")
        private Long courtId;

        @Min(value = 1, message = "Time duration must be at least 1 hour")
        private int timeDuration;
    }

    @Data
    public static class EquipmentBookingDTO {
        @NotNull(message = "Equipment ID is required")
        private Long equipmentId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        @Min(value = 1, message = "Time duration must be at least 1 hour")
        private int timeDuration;
    }

    @Data
    public static class TimeSlotRangeDTO {
        @NotNull(message = "Start time is required")
        private LocalTime startTime;

        @NotNull(message = "End time is required")
        private LocalTime endTime;

        @Min(value = 0, message = "Duration must be positive")
        private double duration; // in hours for this specific range

        // Helper method to validate time range
        public boolean isValidTimeRange() {
            return startTime != null && endTime != null && startTime.isBefore(endTime);
        }
    }

    // Helper methods
    public LocalDateTime getBookingDateTime() {
        return LocalDateTime.of(bookingDate, startTime);
    }

    public boolean isValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    public int getDurationInHours() {
        if (startTime != null && endTime != null) {
            // Calculate duration in minutes first, then convert to hours
            long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
            return (int) Math.round(minutes / 60.0);
        }
        return duration;
    }
}