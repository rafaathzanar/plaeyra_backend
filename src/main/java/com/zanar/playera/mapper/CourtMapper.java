package com.zanar.playera.mapper;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;

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

    // Backward compatibility fields
    dto.setCourtId(court.getCourtId());
    dto.setName(court.getCourtName()); // Alias for courtName
    dto.setSportType(court.getType() != null ? court.getType().name() : null); // Alias for type
    dto.setSurfaceType(null); // Legacy field - Court entity doesn't have surfaceType
    dto.setStatus(court.getStatus() != null ? court.getStatus().name() : null);
    dto.setPricePerHour(court.getPricePerHour() != null ? court.getPricePerHour().doubleValue() : null);
    dto.setDescription(court.getDescription());
    dto.setImageUrl(null); // Legacy field - Court entity doesn't have imageUrl

    // New comprehensive fields
    dto.setCourtName(court.getCourtName());
    dto.setType(court.getType() != null ? court.getType().name() : null);
    dto.setCapacity(court.getCapacity());

    // Court features
    dto.setIsIndoor(court.getIsIndoor());
    dto.setIsLighted(court.getIsLighted());
    dto.setIsAirConditioned(court.getIsAirConditioned());

    // Booking duration
    dto.setMinBookingDuration(court.getMinBookingDuration());
    dto.setMaxBookingDuration(court.getMaxBookingDuration());

    // Time slot management
    dto.setOpeningTime(court.getOpeningTime());
    dto.setClosingTime(court.getClosingTime());
    dto.setSlotDurationMinutes(court.getSlotDurationMinutes());
    dto.setIsActiveOnWeekends(court.getIsActiveOnWeekends());
    dto.setIsActiveOnHolidays(court.getIsActiveOnHolidays());

    // Break times
    dto.setHasBreakTime(court.getHasBreakTime());
    dto.setBreakStartTime(court.getBreakStartTime());
    dto.setBreakEndTime(court.getBreakEndTime());

    // Dynamic pricing
    dto.setDynamicPricingEnabled(court.getDynamicPricingEnabled());
    dto.setPeakHourStart(court.getPeakHourStart());
    dto.setPeakHourEnd(court.getPeakHourEnd());
    dto.setPeakHourMultiplier(court.getPeakHourMultiplier());
    dto.setOffPeakMultiplier(court.getOffPeakMultiplier());
    dto.setWeekendMultiplier(court.getWeekendMultiplier());

    // Maintenance
    dto.setMaintenanceMode(court.getMaintenanceMode());
    dto.setMaintenanceStartTime(court.getMaintenanceStartTime());
    dto.setMaintenanceEndTime(court.getMaintenanceEndTime());

    // Venue information
    if (court.getVenue() != null) {
      dto.setVenueId(court.getVenue().getVenueId());
      dto.setVenueName(court.getVenue().getName());
    }

    return dto;
  }
}