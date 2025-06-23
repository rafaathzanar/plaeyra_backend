package com.zanar.playera.controller;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {
  @Autowired
  private VenueService venueService;

  @GetMapping
  public ResponseEntity<List<VenueResponseDTO>> listVenues() {
    return ResponseEntity.ok(venueService.listVenues());
  }

  @GetMapping("/{id}")
  public ResponseEntity<VenueResponseDTO> getVenueById(@PathVariable Long id) {
    return ResponseEntity.ok(venueService.getVenueById(id));
  }

  @GetMapping("/owner/{ownerId}")
  public ResponseEntity<List<VenueResponseDTO>> listVenuesByOwner(@PathVariable Long ownerId) {
    return ResponseEntity.ok(venueService.listVenuesByOwner(ownerId));
  }

  @PostMapping
  public ResponseEntity<VenueResponseDTO> createVenue(@RequestBody VenueRequestDTO dto) {
    return ResponseEntity.ok(venueService.createVenue(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<VenueResponseDTO> updateVenue(@PathVariable Long id, @RequestBody VenueRequestDTO dto) {
    return ResponseEntity.ok(venueService.updateVenue(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
    venueService.deleteVenue(id);
    return ResponseEntity.noContent().build();
  }
}