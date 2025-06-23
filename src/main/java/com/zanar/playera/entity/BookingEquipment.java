package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingEquipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "booking_id")
  private Booking booking;

  @ManyToOne
  @JoinColumn(name = "equipment_id")
  private Equipment equipment;

  private int quantity;
  private int timeDuration; // in hours
}