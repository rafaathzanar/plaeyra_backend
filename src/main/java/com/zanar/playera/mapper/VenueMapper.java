package com.zanar.playera.mapper;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.entity.Court;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class VenueMapper {
  public static Venue toVenueEntity(VenueRequestDTO dto, VenueOwner owner) {
    Venue venue = new Venue();
    venue.setName(dto.getName());
    venue.setAddress(dto.getAddress());
    venue.setLocation(dto.getLocation());
    venue.setDescription(dto.getDescription());
    venue.setContactNo(dto.getContactNo());
    venue.setEmail(dto.getEmail());
    venue.setWebsite(dto.getWebsite());

    // Handle latitude and longitude
    if (dto.getLatitude() != null && !dto.getLatitude().trim().isEmpty()) {
      venue.setLatitude(Double.parseDouble(dto.getLatitude()));
    }
    if (dto.getLongitude() != null && !dto.getLongitude().trim().isEmpty()) {
      venue.setLongitude(Double.parseDouble(dto.getLongitude()));
    }

    // Handle venue type
    if (dto.getVenueType() != null) {
      venue.setVenueType(Venue.VenueType.valueOf(dto.getVenueType()));
    }

    venue.setMaxCapacity(dto.getMaxCapacity());

    // Handle status
    if (dto.getStatus() != null) {
      venue.setStatus(Venue.VenueStatus.valueOf(dto.getStatus()));
    }

    // Business details
    venue.setOpeningHours(dto.getOpeningHours());
    if (dto.getBasePrice() != null) {
      venue.setBasePrice(dto.getBasePrice().doubleValue());
    }
    venue.setCancellationPolicy(dto.getCancellationPolicy());
    venue.setRefundPolicy(dto.getRefundPolicy());

    // Amenities
    venue.setParkingAvailable(dto.getParkingAvailable());
    venue.setFoodAvailable(dto.getFoodAvailable());
    venue.setChangingRoomsAvailable(dto.getChangingRoomsAvailable());
    venue.setShowerAvailable(dto.getShowerAvailable());
    venue.setWifiAvailable(dto.getWifiAvailable());

    // Lists
    venue.setImages(dto.getImages());
    venue.setAmenities(dto.getAmenities());
    venue.setVenueOwner(owner);

    return venue;
  }

  public static VenueResponseDTO toVenueResponseDTO(Venue venue) {
    VenueResponseDTO dto = new VenueResponseDTO();
    dto.setVenueId(venue.getVenueId());
    dto.setName(venue.getName());
    dto.setAddress(venue.getAddress());
    dto.setLocation(venue.getLocation());
    dto.setDescription(venue.getDescription());
    dto.setContactNo(venue.getContactNo());
    dto.setEmail(venue.getEmail());
    dto.setWebsite(venue.getWebsite());
    dto.setLatitude(venue.getLatitude());
    dto.setLongitude(venue.getLongitude());
    dto.setStatus(venue.getStatus() != null ? venue.getStatus().name() : null);
    dto.setVenueType(venue.getVenueType() != null ? venue.getVenueType().name() : null);
    dto.setMaxCapacity(venue.getMaxCapacity());
    dto.setParkingAvailable(venue.getParkingAvailable());
    dto.setFoodAvailable(venue.getFoodAvailable());
    dto.setChangingRoomsAvailable(venue.getChangingRoomsAvailable());
    dto.setShowerAvailable(venue.getShowerAvailable());
    dto.setWifiAvailable(venue.getWifiAvailable());
    dto.setOpeningHours(venue.getOpeningHours());
    dto.setCancellationPolicy(venue.getCancellationPolicy());
    dto.setRefundPolicy(venue.getRefundPolicy());
    dto.setBasePrice(venue.getBasePrice());
    dto.setDynamicPricingEnabled(venue.getDynamicPricingEnabled());
    dto.setPeakHourMultiplier(venue.getPeakHourMultiplier());
    dto.setOffPeakMultiplier(venue.getOffPeakMultiplier());
    dto.setWeekendMultiplier(venue.getWeekendMultiplier());
    dto.setHolidayMultiplier(venue.getHolidayMultiplier());
    dto.setPeakHourStart(venue.getPeakHourStart());
    dto.setPeakHourEnd(venue.getPeakHourEnd());
    dto.setSpecialEvents(venue.getSpecialEvents());
    dto.setCommissionRate(venue.getCommissionRate());
    dto.setAutoApprovalEnabled(venue.getAutoApprovalEnabled());
    dto.setMinAdvanceBookingHours(venue.getMinAdvanceBookingHours());
    dto.setMaxAdvanceBookingDays(venue.getMaxAdvanceBookingDays());
    dto.setEarliestBookingTime(venue.getEarliestBookingTime());
    dto.setLatestBookingTime(venue.getLatestBookingTime());
    dto.setImages(venue.getImages());
    dto.setAmenities(venue.getAmenities());

    if (venue.getVenueOwner() != null) {
      dto.setOwnerId(venue.getVenueOwner().getUserId());
      dto.setOwnerName(venue.getVenueOwner().getName());
    }

    // Map courts if they exist
    if (venue.getCourts() != null && !venue.getCourts().isEmpty()) {
      dto.setCourts(venue.getCourts().stream()
          .map(court -> {
            CourtResponseDTO courtDto = new CourtResponseDTO();
            courtDto.setCourtId(court.getCourtId());
            courtDto.setName(court.getCourtName()); // Backward compatibility
            courtDto.setType(court.getType() != null ? court.getType().name() : null); // New type field
            courtDto.setSportType(court.getType() != null ? court.getType().name() : null); // Backward compatibility
            courtDto.setSurfaceType(null); // Legacy field
            courtDto.setStatus(court.getStatus() != null ? court.getStatus().name() : null);
            courtDto.setPricePerHour(court.getPricePerHour() != null ? court.getPricePerHour().doubleValue() : null);
            courtDto.setDescription(court.getDescription());
            courtDto.setImageUrl(null); // Legacy field

            // Add additional court fields
            courtDto.setCapacity(court.getCapacity());
            courtDto.setIsIndoor(court.getIsIndoor());
            courtDto.setIsLighted(court.getIsLighted());
            courtDto.setIsAirConditioned(court.getIsAirConditioned());
            courtDto.setMinBookingDuration(court.getMinBookingDuration());
            courtDto.setMaxBookingDuration(court.getMaxBookingDuration());

            // Add time-related fields
            courtDto.setOpeningTime(court.getOpeningTime());
            courtDto.setClosingTime(court.getClosingTime());
            courtDto.setSlotDurationMinutes(court.getSlotDurationMinutes());
            courtDto.setIsActiveOnWeekends(court.getIsActiveOnWeekends());
            courtDto.setIsActiveOnHolidays(court.getIsActiveOnHolidays());

            // Add break time fields
            courtDto.setHasBreakTime(court.getHasBreakTime());
            courtDto.setBreakStartTime(court.getBreakStartTime());
            courtDto.setBreakEndTime(court.getBreakEndTime());

            // Add dynamic pricing fields
            courtDto.setDynamicPricingEnabled(court.getDynamicPricingEnabled());
            courtDto.setPeakHourStart(court.getPeakHourStart());
            courtDto.setPeakHourEnd(court.getPeakHourEnd());
            courtDto.setPeakHourMultiplier(court.getPeakHourMultiplier());
            courtDto.setOffPeakMultiplier(court.getOffPeakMultiplier());
            courtDto.setWeekendMultiplier(court.getWeekendMultiplier());

            // Add maintenance fields
            courtDto.setMaintenanceMode(court.getMaintenanceMode());
            courtDto.setMaintenanceStartTime(court.getMaintenanceStartTime());
            courtDto.setMaintenanceEndTime(court.getMaintenanceEndTime());

            // Add venue information
            courtDto.setVenueId(venue.getVenueId());
            courtDto.setVenueName(venue.getName());

            return courtDto;
          })
          .collect(Collectors.toList()));
    }

    return dto;
  }
}