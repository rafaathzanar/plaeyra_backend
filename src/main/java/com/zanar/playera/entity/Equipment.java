package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long equipmentId;

  private String name;
  private double ratePerHour;
  private int availableQuantity;

  @ManyToOne
  @JoinColumn(name = "court_id")
  private Court court;
}