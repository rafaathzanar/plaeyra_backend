package com.zanar.playera.controller;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.dto.DynamicPricingDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.service.VenueService;
import com.zanar.playera.service.DynamicPricingService;
import com.zanar.playera.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/venues")
@Tag(name = "Venue Management", description = "APIs for venue operations")
@CrossOrigin(origins = "*")
public class VenueController {

  @Autowired
  private VenueService venueService;

  @Autowired
  private DynamicPricingService dynamicPricingService;

  @Autowired
  private NotificationService notificationService;

  @PostMapping
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Create a new venue", description = "Creates a new venue with the provided details")
  public ResponseEntity<VenueResponseDTO> createVenue(@Valid @RequestBody VenueRequestDTO venueRequestDTO) {
    VenueResponseDTO createdVenue = venueService.createVenue(venueRequestDTO);
    return new ResponseEntity<>(createdVenue, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get venue by ID", description = "Retrieves venue details by venue ID")
  public ResponseEntity<VenueResponseDTO> getVenueById(@PathVariable Long id) {
    VenueResponseDTO venue = venueService.getVenueById(id);
    return ResponseEntity.ok(venue);
  }

  @GetMapping
  @Operation(summary = "Get all venues", description = "Retrieves all venues with pagination and filtering")
  public ResponseEntity<Page<VenueResponseDTO>> getAllVenues(
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String sportType,
      @RequestParam(required = false) String venueType,
      @RequestParam(required = false) Double minPrice,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(required = false) Boolean hasParking,
      @RequestParam(required = false) Boolean hasFood,
      @RequestParam(required = false) Boolean hasChangingRooms,
      @RequestParam(required = false) Boolean hasShower,
      @RequestParam(required = false) Boolean hasWifi,
      Pageable pageable) {

    Page<VenueResponseDTO> venues = venueService.getAllVenues(
        location, sportType, venueType, minPrice, maxPrice,
        hasParking, hasFood, hasChangingRooms, hasShower, hasWifi, pageable);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/search")
  @Operation(summary = "Search venues", description = "Search venues by name, description, or address")
  public ResponseEntity<List<VenueResponseDTO>> searchVenues(
      @RequestParam String query,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String sportType) {

    List<VenueResponseDTO> venues = venueService.searchVenues(query, location, sportType);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/nearby")
  @Operation(summary = "Find nearby venues", description = "Find venues within specified radius of coordinates")
  public ResponseEntity<List<VenueResponseDTO>> getNearbyVenues(
      @RequestParam Double latitude,
      @RequestParam Double longitude,
      @RequestParam(defaultValue = "10.0") Double radiusKm) {

    List<VenueResponseDTO> venues = venueService.getNearbyVenues(latitude, longitude, radiusKm);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/owner/{ownerId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venues by owner", description = "Retrieves all venues owned by a specific venue owner")
  public ResponseEntity<List<VenueResponseDTO>> getVenuesByOwner(@PathVariable Long ownerId) {
    List<VenueResponseDTO> venues = venueService.getVenuesByOwner(ownerId);
    return ResponseEntity.ok(venues);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update venue", description = "Updates an existing venue with new details")
  public ResponseEntity<VenueResponseDTO> updateVenue(
      @PathVariable Long id,
      @Valid @RequestBody VenueRequestDTO venueRequestDTO) {

    VenueResponseDTO updatedVenue = venueService.updateVenue(id, venueRequestDTO);
    return ResponseEntity.ok(updatedVenue);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Delete venue", description = "Deletes a venue by ID")
  public ResponseEntity<Void> deleteVenue(@PathVariable Long id) {
    venueService.deleteVenue(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update venue status", description = "Updates the status of a venue")
  public ResponseEntity<VenueResponseDTO> updateVenueStatus(
      @PathVariable Long id,
      @RequestParam String status) {

    VenueResponseDTO updatedVenue = venueService.updateVenueStatus(id, status);
    return ResponseEntity.ok(updatedVenue);
  }

  @PostMapping("/{id}/images")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Add venue images", description = "Adds new images to a venue")
  public ResponseEntity<VenueResponseDTO> addVenueImages(
      @PathVariable Long id,
      @RequestBody List<String> imageUrls) {

    VenueResponseDTO updatedVenue = venueService.addVenueImages(id, imageUrls);
    return ResponseEntity.ok(updatedVenue);
  }

  @DeleteMapping("/{id}/images")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Remove venue images", description = "Removes specific images from a venue")
  public ResponseEntity<VenueResponseDTO> removeVenueImages(
      @PathVariable Long id,
      @RequestBody List<String> imageUrls) {

    VenueResponseDTO updatedVenue = venueService.removeVenueImages(id, imageUrls);
    return ResponseEntity.ok(updatedVenue);
  }

  @PostMapping("/{id}/amenities")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Add venue amenities", description = "Adds new amenities to a venue")
  public ResponseEntity<VenueResponseDTO> addVenueAmenities(
      @PathVariable Long id,
      @RequestBody List<String> amenities) {

    VenueResponseDTO updatedVenue = venueService.addVenueAmenities(id, amenities);
    return ResponseEntity.ok(updatedVenue);
  }

  @DeleteMapping("/{id}/amenities")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Remove venue amenities", description = "Removes specific amenities from a venue")
  public ResponseEntity<VenueResponseDTO> removeVenueAmenities(
      @PathVariable Long id,
      @RequestBody List<String> amenities) {

    VenueResponseDTO updatedVenue = venueService.removeVenueAmenities(id, amenities);
    return ResponseEntity.ok(updatedVenue);
  }

  @PostMapping("/{id}/sports-types")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Add sports types", description = "Adds new sports types supported by the venue")
  public ResponseEntity<VenueResponseDTO> addSportsTypes(
      @PathVariable Long id,
      @RequestBody List<String> sportsTypes) {

    VenueResponseDTO updatedVenue = venueService.addSportsTypes(id, sportsTypes);
    return ResponseEntity.ok(updatedVenue);
  }

  @DeleteMapping("/{id}/sports-types")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Remove sports types", description = "Removes specific sports types from the venue")
  public ResponseEntity<VenueResponseDTO> removeSportsTypes(
      @PathVariable Long id,
      @RequestBody List<String> sportsTypes) {

    VenueResponseDTO updatedVenue = venueService.removeSportsTypes(id, sportsTypes);
    return ResponseEntity.ok(updatedVenue);
  }

  @GetMapping("/{id}/availability")
  @Operation(summary = "Check venue availability", description = "Checks venue availability for specific dates and times")
  public ResponseEntity<Map<String, Object>> checkVenueAvailability(
      @PathVariable Long id,
      @RequestParam String date,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime) {

    Map<String, Object> availability = venueService.checkVenueAvailability(id, date, startTime, endTime);
    return ResponseEntity.ok(availability);
  }

  @GetMapping("/{id}/pricing")
  @Operation(summary = "Get venue pricing", description = "Gets dynamic pricing information for the venue")
  public ResponseEntity<Map<String, Object>> getVenuePricing(@PathVariable Long id) {
    Map<String, Object> pricing = venueService.getVenuePricing(id);
    return ResponseEntity.ok(pricing);
  }

  @PostMapping("/{id}/dynamic-pricing")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update dynamic pricing", description = "Updates dynamic pricing configuration for the venue")
  public ResponseEntity<Void> updateDynamicPricing(
      @PathVariable Long id,
      @RequestBody DynamicPricingDTO dynamicPricingDTO) {

    dynamicPricingService.updateVenueDynamicPricing(id, dynamicPricingDTO);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/analytics")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue analytics", description = "Gets analytics data for the venue")
  public ResponseEntity<Map<String, Object>> getVenueAnalytics(
      @PathVariable Long id,
      @RequestParam(required = false) String period) {

    Map<String, Object> analytics = venueService.getVenueAnalytics(id, period);
    return ResponseEntity.ok(analytics);
  }

  @GetMapping("/{id}/reviews")
  @Operation(summary = "Get venue reviews", description = "Gets all reviews for a specific venue")
  public ResponseEntity<Map<String, Object>> getVenueReviews(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) Integer minRating,
      @RequestParam(required = false) String sortBy) {

    Map<String, Object> reviews = venueService.getVenueReviews(id, page, size, minRating, sortBy);
    return ResponseEntity.ok(reviews);
  }

  @GetMapping("/{id}/average-rating")
  @Operation(summary = "Get venue average rating", description = "Gets the average rating for a venue")
  public ResponseEntity<Map<String, Object>> getVenueAverageRating(@PathVariable Long id) {
    Map<String, Object> rating = venueService.getVenueAverageRating(id);
    return ResponseEntity.ok(rating);
  }

  @PostMapping("/{id}/favorite")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Add venue to favorites", description = "Adds a venue to customer's favorites")
  public ResponseEntity<Void> addToFavorites(@PathVariable Long id) {
    venueService.addToFavorites(id);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}/favorite")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Remove venue from favorites", description = "Removes a venue from customer's favorites")
  public ResponseEntity<Void> removeFromFavorites(@PathVariable Long id) {
    venueService.removeFromFavorites(id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/popular")
  @Operation(summary = "Get popular venues", description = "Gets the most popular venues based on bookings and ratings")
  public ResponseEntity<List<VenueResponseDTO>> getPopularVenues(
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(required = false) String location) {

    List<VenueResponseDTO> venues = venueService.getPopularVenues(limit, location);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/trending")
  @Operation(summary = "Get trending venues", description = "Gets trending venues based on recent activity")
  public ResponseEntity<List<VenueResponseDTO>> getTrendingVenues(
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(required = false) String period) {

    List<VenueResponseDTO> venues = venueService.getTrendingVenues(limit, period);
    return ResponseEntity.ok(venues);
  }
}