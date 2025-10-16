package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "customer", "payment", "bookingCourts", "bookingEquipments", "bookingTimeSlots" })
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    private LocalDateTime bookingDate;

    @CreationTimestamp
    private LocalDateTime createdAt; // Automatically set when booking is created

    private int duration; // in hours
    private double totalCost;
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus = BookingStatus.BOOKED;
    private String specialRequests; // Optional special requests from customer

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    @Fetch(FetchMode.JOIN)
    private Customer customer;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<BookingCourt> bookingCourts;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<BookingEquipment> bookingEquipments;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<BookingTimeSlot> bookingTimeSlots;

    public enum BookingStatus {
        BOOKED, CANCELLED, COMPLETED, NO_SHOW, REFUNDED
    }
}
