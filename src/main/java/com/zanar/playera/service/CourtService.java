package com.zanar.playera.service;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.mapper.CourtMapper;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourtService {
  @Autowired
  private CourtRepository courtRepository;
  @Autowired
  private VenueRepository venueRepository;
  @Autowired
  private UserRepository userRepository;

  public CourtResponseDTO createCourt(CourtRequestDTO dto, Long ownerId) {
    // Validate mandatory fields
    if (dto.getCourtName() == null || dto.getCourtName().isBlank()) {
      throw new RuntimeException("Court name is required");
    }
    if (dto.getType() == null || dto.getType().isBlank()) {
      throw new RuntimeException("Court type is required");
    }
    if (dto.getCapacity() < 1) {
      throw new RuntimeException("Capacity must be at least 1");
    }
    if (dto.getPricePerHour() < 0) {
      throw new RuntimeException("Price per hour must be non-negative");
    }
    Venue venue = venueRepository.findById(dto.getVenueId())
        .orElseThrow(() -> new RuntimeException("Venue not found"));
    // Only allow the owner to add courts
    if (venue.getVenueOwner() == null || !venue.getVenueOwner().getUserId().equals(ownerId)) {
      throw new RuntimeException("You are not authorized to add courts to this venue");
    }
    Court court = CourtMapper.toCourtEntity(dto, venue);
    Court saved = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(saved);
  }

  public CourtResponseDTO getCourtById(Long id) {
    Court court = courtRepository.findById(id).orElseThrow(() -> new RuntimeException("Court not found"));
    return CourtMapper.toCourtResponseDTO(court);
  }

  public List<CourtResponseDTO> listCourts() {
    return courtRepository.findAll().stream().map(CourtMapper::toCourtResponseDTO).collect(Collectors.toList());
  }

  public List<CourtResponseDTO> listCourtsByVenue(Long venueId) {
    return courtRepository.findAll().stream()
        .filter(c -> c.getVenue() != null && c.getVenue().getVenueId().equals(venueId))
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  public CourtResponseDTO updateCourt(Long id, CourtRequestDTO dto, Long ownerId) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));
    Venue venue = court.getVenue();
    // Only allow the owner to update
    if (venue.getVenueOwner() == null || !venue.getVenueOwner().getUserId().equals(ownerId)) {
      throw new RuntimeException("You are not authorized to update this court");
    }
    if (dto.getCourtName() != null && !dto.getCourtName().isBlank()) court.setCourtName(dto.getCourtName());
    if (dto.getType() != null && !dto.getType().isBlank()) court.setType(dto.getType());
    if (dto.getCapacity() > 0) court.setCapacity(dto.getCapacity());
    if (dto.getPricePerHour() >= 0) court.setPricePerHour(dto.getPricePerHour());
    Court saved = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(saved);
  }

  public void deleteCourt(Long id) {
    courtRepository.deleteById(id);
  }
}