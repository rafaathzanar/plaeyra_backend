package com.zanar.playera.mapper;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;

public class CourtMapper {
  public static Court toCourtEntity(CourtRequestDTO dto, Venue venue) {
    Court court = new Court();
    court.setCourtName(dto.getCourtName());
    court.setType(dto.getType());
    court.setCapacity(dto.getCapacity());
    court.setPricePerHour(dto.getPricePerHour());
    court.setVenue(venue);
    return court;
  }

  public static CourtResponseDTO toCourtResponseDTO(Court court) {
    CourtResponseDTO dto = new CourtResponseDTO();
    dto.setCourtId(court.getCourtId());
    dto.setCourtName(court.getCourtName());
    dto.setType(court.getType());
    dto.setCapacity(court.getCapacity());
    dto.setPricePerHour(court.getPricePerHour());
    if (court.getVenue() != null) {
      dto.setVenueId(court.getVenue().getVenueId());
      dto.setVenueName(court.getVenue().getName());
    }
    return dto;
  }
}