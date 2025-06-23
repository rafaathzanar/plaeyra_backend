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
        .filter(v -> v.getOwner() != null && v.getOwner().getUserId().equals(ownerId))
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  public VenueResponseDTO updateVenue(Long id, VenueRequestDTO dto) {
    Venue venue = venueRepository.findById(id).orElseThrow(() -> new RuntimeException("Venue not found"));
    venue.setName(dto.getName());
    venue.setLocation(dto.getLocation());
    venue.setDescription(dto.getDescription());
    venue.setContactNo(dto.getContactNo());
    Venue saved = venueRepository.save(venue);
    return VenueMapper.toVenueResponseDTO(saved);
  }

  public void deleteVenue(Long id) {
    venueRepository.deleteById(id);
  }
}