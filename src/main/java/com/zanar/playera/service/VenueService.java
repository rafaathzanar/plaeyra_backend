package com.zanar.playera.service;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.mapper.VenueMapper;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VenueService {
  @Autowired
  private VenueRepository venueRepository;
  @Autowired
  private UserRepository userRepository;

  public VenueResponseDTO createVenue(VenueRequestDTO dto) {
    // Validate mandatory fields
    if (dto.getName() == null || dto.getName().isBlank()) {
      throw new RuntimeException("Venue name is required");
    }
    if (dto.getAddress() == null || dto.getAddress().isBlank()) {
      throw new RuntimeException("Venue address is required");
    }
    // Only allow venue owners to create venues
    VenueOwner owner = (VenueOwner) userRepository.findById(dto.getOwnerId())
        .orElseThrow(() -> new RuntimeException("Owner not found"));
    Venue venue = VenueMapper.toVenueEntity(dto, owner);
    Venue saved = venueRepository.save(venue);
    return VenueMapper.toVenueResponseDTO(saved);
  }

  public VenueResponseDTO getVenueById(Long id) {
    Venue venue = venueRepository.findById(id).orElseThrow(() -> new RuntimeException("Venue not found"));
    return VenueMapper.toVenueResponseDTO(venue);
  }

  public List<VenueResponseDTO> listVenues() {
    return venueRepository.findAll().stream().map(VenueMapper::toVenueResponseDTO).collect(Collectors.toList());
  }

  public List<VenueResponseDTO> listVenuesByOwner(Long ownerId) {
    return venueRepository.findAll().stream()
        .filter(v -> v.getVenueOwner() != null && v.getVenueOwner().getUserId().equals(ownerId))
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  public VenueResponseDTO updateVenue(Long id, VenueRequestDTO dto) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));
    // Only allow the owner to update
    if (venue.getVenueOwner() == null || !venue.getVenueOwner().getUserId().equals(dto.getOwnerId())) {
      throw new RuntimeException("You are not authorized to update this venue");
    }
    if (dto.getName() != null && !dto.getName().isBlank()) venue.setName(dto.getName());
    if (dto.getAddress() != null && !dto.getAddress().isBlank()) venue.setAddress(dto.getAddress());
    if (dto.getLocation() != null) venue.setLocation(dto.getLocation());
    if (dto.getDescription() != null) venue.setDescription(dto.getDescription());
    if (dto.getContactNo() != null) venue.setContactNo(dto.getContactNo());
    if (dto.getImages() != null) venue.setImages(dto.getImages());
    if (dto.getAmenities() != null) venue.setAmenities(dto.getAmenities());
    Venue saved = venueRepository.save(venue);
    return VenueMapper.toVenueResponseDTO(saved);
  }

  public void deleteVenue(Long id) {
    venueRepository.deleteById(id);
  }
}