package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    public enum SlotStatus {
        AVAILABLE, BOOKED, RESERVED, MAINTENANCE
    }

    // Helper methods
    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE;
    }

    public boolean isBooked() {
        return status == SlotStatus.BOOKED;
    }

    public boolean isReserved() {
        return status == SlotStatus.RESERVED;
    }

    public boolean isUnderMaintenance() {
        return status == SlotStatus.MAINTENANCE;
    }

    public void book(Booking booking) {
        this.booking = booking;
        this.status = SlotStatus.BOOKED;
    }

    public void release() {
        this.booking = null;
        this.status = SlotStatus.AVAILABLE;
    }

    public void reserve() {
        this.status = SlotStatus.RESERVED;
    }
}
