package com.zanar.playera.controller;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;

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

  @GetMapping("/owner/{ownerId}/venue")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue by owner ID", description = "Retrieves the venue associated with a specific venue owner. Each owner can have only one venue.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Owner venue retrieved successfully or no venue found"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Object> getVenueByOwner(
      @Parameter(description = "Unique identifier of the venue owner", required = true) @PathVariable Long ownerId) {
    VenueResponseDTO venue = venueService.getVenueByOwner(ownerId);

    if (venue == null) {
      // Return a proper JSON response indicating no venue exists
      Map<String, Object> emptyVenue = new HashMap<>();
      emptyVenue.put("venueId", null);
      emptyVenue.put("name", null);
      emptyVenue.put("address", null);
      emptyVenue.put("location", null);
      emptyVenue.put("description", null);
      emptyVenue.put("contactNo", null);
      emptyVenue.put("email", null);
      emptyVenue.put("status", null);
      emptyVenue.put("venueType", null);
      emptyVenue.put("maxCapacity", null);
      emptyVenue.put("parkingAvailable", null);
      emptyVenue.put("foodAvailable", null);
      emptyVenue.put("changingRoomsAvailable", null);
      emptyVenue.put("showerAvailable", null);
      emptyVenue.put("wifiAvailable", null);
      emptyVenue.put("basePrice", null);
      emptyVenue.put("images", new ArrayList<>());
      emptyVenue.put("amenities", new ArrayList<>());
      emptyVenue.put("sportsTypes", new ArrayList<>());
      emptyVenue.put("courts", new ArrayList<>());
      emptyVenue.put("equipment", new ArrayList<>());
      emptyVenue.put("venueOwner", null);

      return ResponseEntity.ok(emptyVenue);
    }

    return ResponseEntity.ok(venue);
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