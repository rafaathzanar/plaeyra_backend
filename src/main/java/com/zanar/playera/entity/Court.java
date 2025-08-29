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

    @NotBlank(message = "Sport type is required")
    @Column(nullable = false)
    private String type;

    @Positive(message = "Capacity must be positive")
    @Column(nullable = false)
    private Integer capacity;

    @Positive(message = "Price per hour must be positive")
    @Column(nullable = false)
    private Double pricePerHour;

    private String description;

    @Enumerated(EnumType.STRING)
    private CourtStatus status = CourtStatus.ACTIVE;

    private String surfaceType; // e.g., Wood, Concrete, Grass, Artificial Turf

    private Boolean isIndoor = true;

    private Boolean isLighted = false;

    private Boolean isAirConditioned = false;

    private String equipment; // Comma-separated list of available equipment

    private Integer minBookingDuration = 1; // in hours

    private Integer maxBookingDuration = 24; // in hours

    private Boolean dynamicPricingEnabled = false;

    private Double peakHourMultiplier = 1.5;

    private Double offPeakMultiplier = 0.8;

    private Double weekendMultiplier = 1.2;

    private Double holidayMultiplier = 1.3;

    private LocalTime peakHourStart = LocalTime.of(18, 0); // 6 PM

    private LocalTime peakHourEnd = LocalTime.of(22, 0); // 10 PM

    private String specialEvents; // JSON string for special event pricing

    private Boolean maintenanceMode = false;

    private LocalTime maintenanceStartTime;

    private LocalTime maintenanceEndTime;

    private String maintenanceNotes;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Slot> slots = new ArrayList<>();

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipment> equipmentList = new ArrayList<>();

    @ElementCollection
    private Map<DayOfWeek, CourtAvailability> availabilitySchedule = new HashMap<>();

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
    public double calculateDynamicPrice(LocalTime time, DayOfWeek day) {
        if (!getDynamicPricingEnabled()) {
            return pricePerHour;
        }

        double multiplier = 1.0;

        // Peak hour pricing
        if (time.isAfter(peakHourStart) && time.isBefore(peakHourEnd)) {
            multiplier *= peakHourMultiplier;
        } else {
            multiplier *= offPeakMultiplier;
        }

        // Weekend pricing
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            multiplier *= weekendMultiplier;
        }

        // Check for special day pricing
        CourtAvailability availability = availabilitySchedule.get(day);
        if (availability != null && availability.getSpecialPrice() > 0) {
            return availability.getSpecialPrice();
        }

        return pricePerHour * multiplier;
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
}
