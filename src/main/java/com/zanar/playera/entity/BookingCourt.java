package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCourt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "booking_id")
  private Booking booking;

  @ManyToOne
  @JoinColumn(name = "court_id")
  private Court court;

  private int timeDuration; // in hours
}