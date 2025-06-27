package com.zanar.playera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopFavoritedVenueDTO {
    private Long venueId;
    private String venueName;
    private Long favoriteCount;
} 