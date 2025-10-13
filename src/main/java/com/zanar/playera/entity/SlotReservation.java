package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slot_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotReservation {
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

  @Column(nullable = false)
  private String customerId; // Customer attempting to book

  @Column(nullable = false)
  private LocalDateTime reservedAt;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Enumerated(EnumType.STRING)
  private ReservationStatus status = ReservationStatus.ACTIVE;

  @Version
  private Long version; // Optimistic locking

  public enum ReservationStatus {
    ACTIVE, EXPIRED, CONFIRMED, CANCELLED
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt) || status == ReservationStatus.EXPIRED;
  }

  public boolean isActive() {
    return status == ReservationStatus.ACTIVE && !isExpired();
  }

  public void expire() {
    this.status = ReservationStatus.EXPIRED;
  }

  public void confirm() {
    this.status = ReservationStatus.CONFIRMED;
  }

  public void cancel() {
    this.status = ReservationStatus.CANCELLED;
  }
}
