package com.zanar.playera.controller;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.dto.DynamicPricingDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.service.VenueService;
import com.zanar.playera.service.DynamicPricingService;
import com.zanar.playera.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Venue Management", description = "APIs for comprehensive venue operations including CRUD, search, filtering, analytics, and dynamic pricing management")
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
  @Operation(summary = "Create a new venue", description = "Creates a new venue with comprehensive details including amenities, sports types, and business information. Requires VENUE_OWNER or ADMIN role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Venue created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VenueResponseDTO.class), examples = @ExampleObject(value = """
          {
            "venueId": 1,
            "name": "Elite Sports Complex",
            "address": "123 Sports Street",
            "location": "Downtown",
            "description": "Premium sports facility with multiple courts",
            "contactNo": "+1234567890",
            "email": "info@elitesports.com",
            "status": "ACTIVE",
            "venueType": "INDOOR",
            "maxCapacity": 100,
            "parkingAvailable": true,
            "foodAvailable": true,
            "changingRoomsAvailable": true,
            "showerAvailable": true,
            "wifiAvailable": true,
            "basePrice": 50.0
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid venue data or validation error"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<VenueResponseDTO> createVenue(
      @Parameter(description = "Venue creation details", required = true, content = @Content(examples = @ExampleObject(value = """
          {
            "name": "Elite Sports Complex",
            "address": "123 Sports Street",
            "location": "Downtown",
            "description": "Premium sports facility with multiple courts",
            "contactNo": "+1234567890",
            "email": "info@elitesports.com",
            "venueType": "INDOOR",
            "maxCapacity": 100,
            "parkingAvailable": true,
            "foodAvailable": true,
            "changingRoomsAvailable": true,
            "showerAvailable": true,
            "wifiAvailable": true,
            "basePrice": 50.0
          }
          """))) @Valid @RequestBody VenueRequestDTO venueRequestDTO) {
    VenueResponseDTO createdVenue = venueService.createVenue(venueRequestDTO);
    return new ResponseEntity<>(createdVenue, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get venue by ID", description = "Retrieves comprehensive venue details by venue ID including amenities, sports types, and current status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Venue details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VenueResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  // Public endpoint - no authentication required
  public ResponseEntity<VenueResponseDTO> getVenueById(
      @Parameter(description = "Unique identifier of the venue", required = true) @PathVariable Long id) {
    VenueResponseDTO venue = venueService.getVenueById(id);
    return ResponseEntity.ok(venue);
  }

  @GetMapping
  @Operation(summary = "Get all venues with advanced filtering", description = "Retrieves all venues with comprehensive filtering options including location, sport type, amenities, price range, and pagination support.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Venues retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
  })
  // Public endpoint - no authentication required
  public ResponseEntity<Page<VenueResponseDTO>> getAllVenues(
      @Parameter(description = "Filter by venue location (city, area)") @RequestParam(required = false) String location,
      @Parameter(description = "Filter by supported sport type") @RequestParam(required = false) String sportType,
      @Parameter(description = "Filter by venue type (INDOOR, OUTDOOR, MIXED, SPECIALIZED)") @RequestParam(required = false) String venueType,
      @Parameter(description = "Minimum price per hour") @RequestParam(required = false) Double minPrice,
      @Parameter(description = "Maximum price per hour") @RequestParam(required = false) Double maxPrice,
      @Parameter(description = "Filter by parking availability") @RequestParam(required = false) Boolean hasParking,
      @Parameter(description = "Filter by food availability") @RequestParam(required = false) Boolean hasFood,
      @Parameter(description = "Filter by changing rooms availability") @RequestParam(required = false) Boolean hasChangingRooms,
      @Parameter(description = "Filter by shower availability") @RequestParam(required = false) Boolean hasShower,
      @Parameter(description = "Filter by WiFi availability") @RequestParam(required = false) Boolean hasWifi,
      @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {

    Page<VenueResponseDTO> venues = venueService.getAllVenues(
        location, sportType, venueType, minPrice, maxPrice,
        hasParking, hasFood, hasChangingRooms, hasShower, hasWifi, pageable);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/search")
  @Operation(summary = "Search venues by text query", description = "Search venues by name, description, or address with optional location and sport type filtering.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
  })
  public ResponseEntity<List<VenueResponseDTO>> searchVenues(
      @Parameter(description = "Search query (venue name, description, or address)", required = true) @RequestParam String query,
      @Parameter(description = "Optional location filter") @RequestParam(required = false) String location,
      @Parameter(description = "Optional sport type filter") @RequestParam(required = false) String sportType) {

    List<VenueResponseDTO> venues = venueService.searchVenues(query, location, sportType);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/nearby")
  @Operation(summary = "Find nearby venues by coordinates", description = "Find venues within a specified radius (in kilometers) from given latitude and longitude coordinates using Haversine distance calculation.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Nearby venues retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
  })
  public ResponseEntity<List<VenueResponseDTO>> getNearbyVenues(
      @Parameter(description = "Latitude coordinate", required = true, example = "40.7128") @RequestParam Double latitude,
      @Parameter(description = "Longitude coordinate", required = true, example = "-74.0060") @RequestParam Double longitude,
      @Parameter(description = "Search radius in kilometers", example = "10.0") @RequestParam(defaultValue = "10.0") Double radiusKm) {

    List<VenueResponseDTO> venues = venueService.getNearbyVenues(latitude, longitude, radiusKm);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/owner/{ownerId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venues by owner", description = "Retrieves all venues owned by a specific venue owner. Requires VENUE_OWNER or ADMIN role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Owner venues retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<VenueResponseDTO>> getVenuesByOwner(
      @Parameter(description = "Unique identifier of the venue owner", required = true) @PathVariable Long ownerId) {
    List<VenueResponseDTO> venues = venueService.getVenuesByOwner(ownerId);
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/owner/{ownerId}/venue")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue by owner", description = "Retrieves the single venue owned by a specific venue owner. Requires VENUE_OWNER or ADMIN role.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Owner venue retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "Venue not found for owner"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<VenueResponseDTO> getVenueByOwner(
      @Parameter(description = "Unique identifier of the venue owner", required = true) @PathVariable Long ownerId) {
    VenueResponseDTO venue = venueService.getVenueByOwner(ownerId);
    return ResponseEntity.ok(venue);
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