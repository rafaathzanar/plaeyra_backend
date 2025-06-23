package com.zanar.playera.service;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.mapper.CourtMapper;
import com.zanar.playera.repo.CourtRepository;
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

  public CourtResponseDTO createCourt(CourtRequestDTO dto) {
    Venue venue = venueRepository.findById(dto.getVenueId())
        .orElseThrow(() -> new RuntimeException("Venue not found"));
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

  public CourtResponseDTO updateCourt(Long id, CourtRequestDTO dto) {
    Court court = courtRepository.findById(id).orElseThrow(() -> new RuntimeException("Court not found"));
    court.setCourtName(dto.getCourtName());
    court.setType(dto.getType());
    court.setCapacity(dto.getCapacity());
    court.setPricePerHour(dto.getPricePerHour());
    Court saved = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(saved);
  }

  public void deleteCourt(Long id) {
    courtRepository.deleteById(id);
  }
}