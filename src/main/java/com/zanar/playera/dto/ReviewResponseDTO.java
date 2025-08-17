package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponseDTO {
  private Long reviewId;
  private String comment;
  private int rating;
  private LocalDateTime reviewDate;
  private Long customerId;
  private String customerName;
  private Long venueId;
  private String venueName;
}