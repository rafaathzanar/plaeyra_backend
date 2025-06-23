package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteRequestDTO {
  private LocalDateTime addedDate;
  private Long customerId;
  private Long venueId;
}