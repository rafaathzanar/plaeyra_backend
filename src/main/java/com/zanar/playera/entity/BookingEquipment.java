package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_equipment")
public class BookingEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private int quantity;
    
    @Column(nullable = false)
    private int timeDuration; // in hours
    
    @Column(nullable = false)
    private double unitPrice; // price per hour at time of booking
    
    @Column(nullable = false)
    private double totalPrice; // total cost for this equipment rental
    
    @Column(nullable = false)
    private double depositAmount; // deposit amount for this equipment
    
    @Enumerated(EnumType.STRING)
    private RentalStatus status = RentalStatus.RENTED;
    
    private LocalDateTime returnDate;
    
    private String returnNotes; // for damage reports, etc.
    
    private Boolean isReturned = false;
    
    public enum RentalStatus {
        RENTED, RETURNED, DAMAGED, LOST, REFUNDED
    }
    
    // Helper methods
    public boolean isReturned() {
        return status == RentalStatus.RETURNED && isReturned;
    }
    
    public boolean isDamaged() {
        return status == RentalStatus.DAMAGED;
    }
    
    public boolean isLost() {
        return status == RentalStatus.LOST;
    }
    
    public boolean isRefunded() {
        return status == RentalStatus.REFUNDED;
    }
    
    public double calculateRefundAmount(double refundPercentage) {
        return totalPrice * refundPercentage;
    }
    
    public void markAsReturned() {
        this.status = RentalStatus.RETURNED;
        this.isReturned = true;
        this.returnDate = LocalDateTime.now();
    }
    
    public void markAsDamaged(String notes) {
        this.status = RentalStatus.DAMAGED;
        this.returnNotes = notes;
        this.returnDate = LocalDateTime.now();
    }
    
    public void markAsLost() {
        this.status = RentalStatus.LOST;
        this.returnDate = LocalDateTime.now();
    }
    
    public void markAsRefunded() {
        this.status = RentalStatus.REFUNDED;
    }
}