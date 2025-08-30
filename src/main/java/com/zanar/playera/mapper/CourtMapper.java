package com.zanar.playera.mapper;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;
import org.springframework.stereotype.Component;

@Component
public class CourtMapper {
  public static Court toCourtEntity(CourtRequestDTO dto, Venue venue) {
    Court court = new Court();
    court.setCourtName(dto.getCourtName());
    court.setType(Court.CourtType.valueOf(dto.getType().toUpperCase()));
    court.setCapacity(dto.getCapacity());
    court.setPricePerHour(java.math.BigDecimal.valueOf(dto.getPricePerHour()));
    court.setVenue(venue);
    return court;
  }

  public static CourtResponseDTO toCourtResponseDTO(Court court) {
    CourtResponseDTO dto = new CourtResponseDTO();
    dto.setCourtId(court.getCourtId());
    dto.setCourtName(court.getCourtName());
    dto.setType(court.getType().name());
    dto.setCapacity(court.getCapacity());
    dto.setPricePerHour(court.getPricePerHour().doubleValue());
    if (court.getVenue() != null) {
      dto.setVenueId(court.getVenue().getVenueId());
      dto.setVenueName(court.getVenue().getName());
    }
    return dto;
  }
}