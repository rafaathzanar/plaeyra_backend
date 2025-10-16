package com.zanar.playera.controller;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    List<VenueResponseDTO> venues = venueService.listVenues();

    // Debug logging for images
    System.out.println("=== DEBUG: List Venues API Response ===");
    System.out.println("Total venues: " + venues.size());
    for (VenueResponseDTO venue : venues) {
      System.out.println("Venue: " + venue.getName() + " - Images: " + venue.getImages() + " (Count: "
          + (venue.getImages() != null ? venue.getImages().size() : 0) + ")");
    }
    System.out.println("=====================================");

    return ResponseEntity.ok(venues);
  }

  @GetMapping("/{id}")
  public ResponseEntity<VenueResponseDTO> getVenueById(@PathVariable Long id) {
    VenueResponseDTO venue = venueService.getVenueById(id);
    
    // Debug logging for images
    System.out.println("=== DEBUG: Get Venue By ID API Response ===");
    System.out.println("Venue ID: " + id);
    System.out.println("Venue: " + venue.getName() + " - Images: " + venue.getImages() + " (Count: "
        + (venue.getImages() != null ? venue.getImages().size() : 0) + ")");
    System.out.println("==========================================");
    
    return ResponseEntity.ok(venue);
  }

  @GetMapping("/test-images/{id}")
  public ResponseEntity<Map<String, Object>> testImages(@PathVariable Long id) {
    Map<String, Object> response = new HashMap<>();

    try {
      VenueResponseDTO venue = venueService.getVenueById(id);
      response.put("venueId", venue.getVenueId());
      response.put("venueName", venue.getName());
      response.put("images", venue.getImages());
      response.put("imagesCount", venue.getImages() != null ? venue.getImages().size() : 0);
      response.put("imagesType", venue.getImages() != null ? venue.getImages().getClass().getSimpleName() : "null");

      // Also check courts
      if (venue.getCourts() != null && !venue.getCourts().isEmpty()) {
        CourtResponseDTO firstCourt = venue.getCourts().get(0);
        response.put("firstCourtId", firstCourt.getCourtId());
        response.put("firstCourtName", firstCourt.getCourtName());
        response.put("firstCourtImages", firstCourt.getImages());
        response.put("firstCourtImagesCount", firstCourt.getImages() != null ? firstCourt.getImages().size() : 0);
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("error", e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
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
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Create a new venue", description = "Creates a new sports venue. Only venue owners and admins can create venues.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Venue created successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "400", description = "Invalid venue data")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<VenueResponseDTO> createVenue(@RequestBody VenueRequestDTO dto) {
    // Add debug logging
    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();

    if (auth != null) {
      System.out.println("=== DEBUG: Venue Creation ===");
      System.out.println("User: " + auth.getName());
      System.out.println("Authorities: " + auth.getAuthorities());
      System.out.println("Principal: " + auth.getPrincipal());
      System.out.println("Venue Data: " + dto);
      System.out.println("===============================");
    }

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