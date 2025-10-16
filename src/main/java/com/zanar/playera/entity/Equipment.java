package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "equipment")
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long equipmentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private double ratePerHour;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    private int minimumRentalHours = 1;

    private int maximumRentalHours = 24;

    @ManyToOne
    @JoinColumn(name = "court_id")
    private Court court;

    private LocalDateTime lastMaintenanceDate;

    public enum EquipmentStatus {
        AVAILABLE, MAINTENANCE, OUT_OF_SERVICE, RESERVED
    }

    // Helper methods
    public boolean isAvailable() {
        return status == EquipmentStatus.AVAILABLE && availableQuantity > 0;
    }

    public boolean isUnderMaintenance() {
        return status == EquipmentStatus.MAINTENANCE;
    }

    public boolean isOutOfService() {
        return status == EquipmentStatus.OUT_OF_SERVICE;
    }

    public boolean isReserved() {
        return status == EquipmentStatus.RESERVED;
    }

    public int getRentedQuantity() {
        return totalQuantity - availableQuantity;
    }

    public double calculateRentalCost(int quantity, int durationHours) {
        return ratePerHour * quantity * durationHours;
    }

    public boolean canRent(int requestedQuantity, int durationHours) {
        return isAvailable() &&
                availableQuantity >= requestedQuantity &&
                durationHours >= minimumRentalHours &&
                durationHours <= maximumRentalHours;
    }

    public void reserve(int quantity) {
        if (availableQuantity >= quantity) {
            availableQuantity -= quantity;
            if (availableQuantity == 0) {
                status = EquipmentStatus.RESERVED;
            }
        } else {
            throw new RuntimeException("Not enough equipment available for reservation");
        }
    }

    public void release(int quantity) {
        availableQuantity += quantity;
        if (status == EquipmentStatus.RESERVED && availableQuantity > 0) {
            status = EquipmentStatus.AVAILABLE;
        }
    }
}