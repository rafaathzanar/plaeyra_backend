package com.zanar.playera.controller;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.service.CourtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courts")
public class CourtController {
  @Autowired
  private CourtService courtService;

  @GetMapping
  public ResponseEntity<List<CourtResponseDTO>> listCourts() {
    return ResponseEntity.ok(courtService.listCourts());
  }

  @GetMapping("/{id}")
  public ResponseEntity<CourtResponseDTO> getCourtById(@PathVariable Long id) {
    return ResponseEntity.ok(courtService.getCourtById(id));
  }

  @GetMapping("/venue/{venueId}")
  public ResponseEntity<List<CourtResponseDTO>> listCourtsByVenue(@PathVariable Long venueId) {
    return ResponseEntity.ok(courtService.listCourtsByVenue(venueId));
  }

  @PostMapping
  public ResponseEntity<CourtResponseDTO> createCourt(@RequestBody CourtRequestDTO dto) {
    return ResponseEntity.ok(courtService.createCourt(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<CourtResponseDTO> updateCourt(@PathVariable Long id, @RequestBody CourtRequestDTO dto) {
    return ResponseEntity.ok(courtService.updateCourt(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
    courtService.deleteCourt(id);
    return ResponseEntity.noContent().build();
  }
}