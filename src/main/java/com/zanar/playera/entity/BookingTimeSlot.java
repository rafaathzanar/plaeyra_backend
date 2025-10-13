package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "booking_time_slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "court_id", "startTime", "endTime", "booking_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingTimeSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "court_id", nullable = false)
  private Court court;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Column(nullable = false)
  private double duration; // in hours

  @Column(nullable = false)
  private double cost; // cost for this specific time range

  @Version
  private Long version; // Optimistic locking

  // Helper methods
  public boolean overlapsWith(LocalTime otherStart, LocalTime otherEnd) {
    return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
  }

  public boolean isConsecutiveWith(BookingTimeSlot other) {
    return this.endTime.equals(other.startTime) || other.endTime.equals(this.startTime);
  }
}
