package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class FavoriteRequestDTO {
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Venue ID is required")
    private Long venueId;
    
    // Optional: if not provided, will be set to current time
    private LocalDateTime addedDate;
}