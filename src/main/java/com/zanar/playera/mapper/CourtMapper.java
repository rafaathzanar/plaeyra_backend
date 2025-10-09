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

    // Set additional fields if provided
    if (dto.getDescription() != null) {
      court.setDescription(dto.getDescription());
    }
    if (dto.getIsIndoor() != null) {
      court.setIsIndoor(dto.getIsIndoor());
    }
    if (dto.getIsLighted() != null) {
      court.setIsLighted(dto.getIsLighted());
    }
    if (dto.getIsAirConditioned() != null) {
      court.setIsAirConditioned(dto.getIsAirConditioned());
    }
    if (dto.getMinBookingDuration() != null) {
      court.setMinBookingDuration(dto.getMinBookingDuration());
    }
    if (dto.getMaxBookingDuration() != null) {
      court.setMaxBookingDuration(dto.getMaxBookingDuration());
    }
    if (dto.getOpeningTime() != null) {
      court.setOpeningTime(dto.getOpeningTime());
    }
    if (dto.getClosingTime() != null) {
      court.setClosingTime(dto.getClosingTime());
    }
    if (dto.getSlotDurationMinutes() != null) {
      court.setSlotDurationMinutes(dto.getSlotDurationMinutes());
    }
    if (dto.getIsActiveOnWeekends() != null) {
      court.setIsActiveOnWeekends(dto.getIsActiveOnWeekends());
    }
    if (dto.getIsActiveOnHolidays() != null) {
      court.setIsActiveOnHolidays(dto.getIsActiveOnHolidays());
    }
    if (dto.getHasBreakTime() != null) {
      court.setHasBreakTime(dto.getHasBreakTime());
    }
    if (dto.getBreakStartTime() != null) {
      court.setBreakStartTime(dto.getBreakStartTime());
    }
    if (dto.getBreakEndTime() != null) {
      court.setBreakEndTime(dto.getBreakEndTime());
    }
    if (dto.getDynamicPricingEnabled() != null) {
      court.setDynamicPricingEnabled(dto.getDynamicPricingEnabled());
    }
    if (dto.getPeakHourStart() != null) {
      court.setPeakHourStart(dto.getPeakHourStart());
    }
    if (dto.getPeakHourEnd() != null) {
      court.setPeakHourEnd(dto.getPeakHourEnd());
    }
    if (dto.getPeakHourMultiplier() != null) {
      court.setPeakHourMultiplier(dto.getPeakHourMultiplier());
    }
    if (dto.getOffPeakMultiplier() != null) {
      court.setOffPeakMultiplier(dto.getOffPeakMultiplier());
    }
    if (dto.getWeekendMultiplier() != null) {
      court.setWeekendMultiplier(dto.getWeekendMultiplier());
    }
    if (dto.getMaintenanceMode() != null) {
      court.setMaintenanceMode(dto.getMaintenanceMode());
    }
    if (dto.getMaintenanceStartTime() != null) {
      court.setMaintenanceStartTime(dto.getMaintenanceStartTime());
    }
    if (dto.getMaintenanceEndTime() != null) {
      court.setMaintenanceEndTime(dto.getMaintenanceEndTime());
    }
    if (dto.getImages() != null) {
      court.setImages(dto.getImages());
    }

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

    // Images
    dto.setImages(court.getImages());

    return dto;
  }
}