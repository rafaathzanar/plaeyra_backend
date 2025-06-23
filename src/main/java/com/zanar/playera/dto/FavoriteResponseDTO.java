package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteResponseDTO {
  private Long favoriteId;
  private LocalDateTime addedDate;
  private Long customerId;
  private String customerName;
  private Long venueId;
  private String venueName;
}