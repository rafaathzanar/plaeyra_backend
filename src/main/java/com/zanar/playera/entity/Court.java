package com.zanar.playera.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "courts")
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courtId;

    @NotBlank(message = "Court name is required")
    @Column(nullable = false)
    private String courtName;

    @Enumerated(EnumType.STRING)
    private CourtType type;

    private Integer capacity;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    private String description;

    private Boolean isIndoor;
    private Boolean isLighted;
    private Boolean isAirConditioned;

    private Integer minBookingDuration; // in hours
    private Integer maxBookingDuration; // in hours

    @Enumerated(EnumType.STRING)
    private CourtStatus status;

    // Time slot management fields
    private LocalTime openingTime; // e.g., 06:00
    private LocalTime closingTime; // e.g., 23:00
    private Integer slotDurationMinutes; // e.g., 30 for 30-minute slots
    private Boolean isActiveOnWeekends;
    private Boolean isActiveOnHolidays;

    // Break times (for maintenance, cleaning, etc.)
    private LocalTime breakStartTime; // e.g., 12:00 for lunch break
    private LocalTime breakEndTime; // e.g., 13:00
    private Boolean hasBreakTime;

    // Dynamic pricing fields
    private Boolean dynamicPricingEnabled;
    private LocalTime peakHourStart;
    private LocalTime peakHourEnd;
    private Double peakHourMultiplier;
    private Double offPeakMultiplier;
    private Double weekendMultiplier;

    // Maintenance fields
    private Boolean maintenanceMode;
    private LocalTime maintenanceStartTime;
    private LocalTime maintenanceEndTime;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Slot> slots = new ArrayList<>();

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipment> equipmentList = new ArrayList<>();

    @ElementCollection
    private Map<DayOfWeek, CourtAvailability> availabilitySchedule = new HashMap<>();

    public enum CourtType {
        BASKETBALL, FUTSAL, BADMINTON, TENNIS, CRICKET, MULTI_SPORT, VOLLEYBALL, SOCCER
    }

    public enum CourtStatus {
        ACTIVE, INACTIVE, MAINTENANCE, RESERVED, DELETED
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtAvailability {
        private LocalTime openTime;
        private LocalTime closeTime;
        private Boolean isAvailable;
        private String specialNotes;
        private Double specialPrice; // Override price for specific days
    }

    // Helper methods for dynamic pricing
    public BigDecimal calculateDynamicPrice(LocalTime time, DayOfWeek day) {
        if (!getDynamicPricingEnabled()) {
            return pricePerHour;
        }

        BigDecimal multiplier = BigDecimal.ONE;

        // Peak hour pricing
        if (time.isAfter(peakHourStart) && time.isBefore(peakHourEnd)) {
            multiplier = multiplier.multiply(BigDecimal.valueOf(peakHourMultiplier));
        } else {
            multiplier = multiplier.multiply(BigDecimal.valueOf(offPeakMultiplier));
        }

        // Weekend pricing
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            multiplier = multiplier.multiply(BigDecimal.valueOf(weekendMultiplier));
        }

        // Check for special day pricing
        CourtAvailability availability = availabilitySchedule.get(day);
        if (availability != null && availability.getSpecialPrice() > 0) {
            return BigDecimal.valueOf(availability.getSpecialPrice());
        }

        return pricePerHour.multiply(multiplier);
    }

    public boolean isAvailable(DayOfWeek day, LocalTime time) {
        if (status != CourtStatus.ACTIVE || getMaintenanceMode()) {
            return false;
        }

        CourtAvailability availability = availabilitySchedule.get(day);
        if (availability == null || !availability.getIsAvailable()) {
            return false;
        }

        return time.isAfter(availability.getOpenTime()) && time.isBefore(availability.getCloseTime());
    }

    public boolean isUnderMaintenance(LocalTime time) {
        if (!getMaintenanceMode()) {
            return false;
        }

        return time.isAfter(maintenanceStartTime) && time.isBefore(maintenanceEndTime);
    }

    public boolean canBookDuration(int duration) {
        return duration >= minBookingDuration && duration <= maxBookingDuration;
    }

    public void addSlot(Slot slot) {
        slots.add(slot);
        slot.setCourt(this);
    }

    public void removeSlot(Slot slot) {
        slots.remove(slot);
        slot.setCourt(null);
    }

    public void addEquipment(Equipment equipment) {
        equipmentList.add(equipment);
        equipment.setCourt(this);
    }

    public void removeEquipment(Equipment equipment) {
        equipmentList.remove(equipment);
        equipment.setCourt(null);
    }

    public List<Slot> getAvailableSlots() {
        return slots.stream()
                .filter(Slot::isAvailable)
                .toList();
    }

    public List<Slot> getBookedSlots() {
        return slots.stream()
                .filter(Slot::isBooked)
                .toList();
    }

    public double getOccupancyRate() {
        if (slots.isEmpty()) {
            return 0.0;
        }

        long bookedSlots = slots.stream()
                .filter(Slot::isBooked)
                .count();

        return (double) bookedSlots / slots.size();
    }

    // Helper methods for time slot management
    public boolean isOpenAt(LocalTime time) {
        if (time == null || openingTime == null || closingTime == null) {
            return false;
        }

        // Check if time is within operating hours
        boolean withinHours = !time.isBefore(openingTime) && !time.isAfter(closingTime);

        // Check if time is during break time
        if (hasBreakTime && breakStartTime != null && breakEndTime != null) {
            boolean duringBreak = !time.isBefore(breakStartTime) && !time.isAfter(breakEndTime);
            return withinHours && !duringBreak;
        }

        return withinHours;
    }

    public int getTotalSlotsPerDay() {
        if (openingTime == null || closingTime == null || slotDurationMinutes == null) {
            return 0;
        }

        long totalMinutes = java.time.Duration.between(openingTime, closingTime).toMinutes();

        // Subtract break time if exists
        if (hasBreakTime && breakStartTime != null && breakEndTime != null) {
            long breakMinutes = java.time.Duration.between(breakStartTime, breakEndTime).toMinutes();
            totalMinutes -= breakMinutes;
        }

        return (int) (totalMinutes / slotDurationMinutes);
    }
}
