package com.zanar.playera.mapper;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;

public class VenueMapper {
  public static Venue toVenueEntity(VenueRequestDTO dto, VenueOwner owner) {
    Venue venue = new Venue();
    venue.setName(dto.getName());
    venue.setAddress(dto.getAddress());
    venue.setLocation(dto.getLocation());
    venue.setDescription(dto.getDescription());
    venue.setContactNo(dto.getContactNo());
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
    dto.setImages(venue.getImages());
    dto.setAmenities(venue.getAmenities());
    if (venue.getVenueOwner() != null) {
      dto.setOwnerId(venue.getVenueOwner().getUserId());
      dto.setOwnerName(venue.getVenueOwner().getName());
    }
    return dto;
  }
}