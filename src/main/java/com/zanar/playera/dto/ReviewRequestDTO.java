package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewRequestDTO {
  private String comment;
  private int rating;
  private LocalDateTime reviewDate;
  private Long customerId;
  private Long venueId;
}