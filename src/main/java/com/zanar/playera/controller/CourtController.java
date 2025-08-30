package com.zanar.playera.controller;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.dto.DynamicPricingDTO;
import com.zanar.playera.service.CourtService;
import com.zanar.playera.service.DynamicPricingService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courts")
@Tag(name = "Court Management", description = "APIs for court operations")
@CrossOrigin(origins = "*")
public class CourtController {

  @Autowired
  private CourtService courtService;

  @Autowired
  private DynamicPricingService dynamicPricingService;

  @PostMapping
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Create a new court", description = "Creates a new court within a venue")
  public ResponseEntity<CourtResponseDTO> createCourt(@Valid @RequestBody CourtRequestDTO courtRequestDTO) {
    CourtResponseDTO createdCourt = courtService.createCourt(courtRequestDTO);
    return new ResponseEntity<>(createdCourt, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get court by ID", description = "Retrieves court details by court ID")
  public ResponseEntity<CourtResponseDTO> getCourtById(@PathVariable Long id) {
    CourtResponseDTO court = courtService.getCourtById(id);
    return ResponseEntity.ok(court);
  }

  @GetMapping
  @Operation(summary = "Get all courts", description = "Retrieves all courts with pagination and filtering")
  public ResponseEntity<Page<CourtResponseDTO>> getAllCourts(
      @RequestParam(required = false) Long venueId,
      @RequestParam(required = false) String sportType,

      @RequestParam(required = false) Boolean isIndoor,
      @RequestParam(required = false) Boolean isLighted,
      @RequestParam(required = false) Boolean isAirConditioned,
      @RequestParam(required = false) Double minPrice,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(required = false) String status,
      Pageable pageable) {

    Page<CourtResponseDTO> courts = courtService.getAllCourts(
        venueId, sportType, isIndoor, isLighted,
        isAirConditioned, minPrice, maxPrice, status, pageable);
    return ResponseEntity.ok(courts);
  }

  @GetMapping("/venue/{venueId}")
  @Operation(summary = "Get courts by venue", description = "Retrieves all courts for a specific venue")
  public ResponseEntity<List<CourtResponseDTO>> getCourtsByVenue(@PathVariable Long venueId) {
    List<CourtResponseDTO> courts = courtService.getCourtsByVenue(venueId);
    return ResponseEntity.ok(courts);
  }

  @GetMapping("/search")
  @Operation(summary = "Search courts", description = "Search courts by name, description, or sport type")
  public ResponseEntity<List<CourtResponseDTO>> searchCourts(
      @RequestParam String query,
      @RequestParam(required = false) Long venueId,
      @RequestParam(required = false) String sportType) {

    List<CourtResponseDTO> courts = courtService.searchCourts(query, venueId, sportType);
    return ResponseEntity.ok(courts);
  }

  @GetMapping("/available")
  @Operation(summary = "Find available courts", description = "Find available courts for specific date and time")
  public ResponseEntity<List<CourtResponseDTO>> getAvailableCourts(
      @RequestParam String date,
      @RequestParam String startTime,
      @RequestParam String endTime,
      @RequestParam(required = false) Long venueId,
      @RequestParam(required = false) String sportType) {

    List<CourtResponseDTO> courts = courtService.getAvailableCourts(
        LocalDate.parse(date), LocalTime.parse(startTime),
        LocalTime.parse(endTime), venueId, sportType);
    return ResponseEntity.ok(courts);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update court", description = "Updates an existing court with new details")
  public ResponseEntity<CourtResponseDTO> updateCourt(
      @PathVariable Long id,
      @Valid @RequestBody CourtRequestDTO courtRequestDTO) {

    CourtResponseDTO updatedCourt = courtService.updateCourt(id, courtRequestDTO);
    return ResponseEntity.ok(updatedCourt);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Delete court", description = "Deletes a court by ID")
  public ResponseEntity<Void> deleteCourt(@PathVariable Long id) {
    courtService.deleteCourt(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update court status", description = "Updates the status of a court")
  public ResponseEntity<CourtResponseDTO> updateCourtStatus(
      @PathVariable Long id,
      @RequestParam String status) {

    CourtResponseDTO updatedCourt = courtService.updateCourtStatus(id, status);
    return ResponseEntity.ok(updatedCourt);
  }

  @PostMapping("/{id}/maintenance")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Set court maintenance", description = "Sets a court to maintenance mode")
  public ResponseEntity<CourtResponseDTO> setMaintenanceMode(
      @PathVariable Long id,
      @RequestParam String startTime,
      @RequestParam String endTime,
      @RequestParam(required = false) String notes) {

    CourtResponseDTO updatedCourt = courtService.setMaintenanceMode(
        id, LocalTime.parse(startTime), LocalTime.parse(endTime), notes);
    return ResponseEntity.ok(updatedCourt);
  }

  @DeleteMapping("/{id}/maintenance")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Remove court maintenance", description = "Removes maintenance mode from a court")
  public ResponseEntity<CourtResponseDTO> removeMaintenanceMode(@PathVariable Long id) {
    CourtResponseDTO updatedCourt = courtService.removeMaintenanceMode(id);
    return ResponseEntity.ok(updatedCourt);
  }

  @GetMapping("/{id}/availability")
  @Operation(summary = "Check court availability", description = "Checks court availability for specific dates and times")
  public ResponseEntity<Map<String, Object>> checkCourtAvailability(
      @PathVariable Long id,
      @RequestParam String date,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime) {

    Map<String, Object> availability = courtService.checkCourtAvailability(
        id, LocalDate.parse(date),
        startTime != null ? LocalTime.parse(startTime) : null,
        endTime != null ? LocalTime.parse(endTime) : null);
    return ResponseEntity.ok(availability);
  }

  @GetMapping("/{id}/pricing")
  @Operation(summary = "Get court pricing", description = "Gets dynamic pricing information for the court")
  public ResponseEntity<Map<String, Object>> getCourtPricing(@PathVariable Long id) {
    Map<String, Object> pricing = courtService.getCourtPricing(id);
    return ResponseEntity.ok(pricing);
  }

  @PostMapping("/{id}/dynamic-pricing")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Update dynamic pricing", description = "Updates dynamic pricing configuration for the court")
  public ResponseEntity<Void> updateDynamicPricing(
      @PathVariable Long id,
      @RequestBody DynamicPricingDTO dynamicPricingDTO) {

    dynamicPricingService.updateCourtDynamicPricing(id, dynamicPricingDTO);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/slots")
  @Operation(summary = "Get court time slots", description = "Gets all time slots for a court")
  public ResponseEntity<Map<String, Object>> getCourtSlots(
      @PathVariable Long id,
      @RequestParam String date) {

    Map<String, Object> slots = courtService.getCourtSlots(id, LocalDate.parse(date));
    return ResponseEntity.ok(slots);
  }

  @GetMapping("/{id}/equipment")
  @Operation(summary = "Get court equipment", description = "Gets all equipment available for a court")
  public ResponseEntity<List<Map<String, Object>>> getCourtEquipment(@PathVariable Long id) {
    List<Map<String, Object>> equipment = courtService.getCourtEquipment(id);
    return ResponseEntity.ok(equipment);
  }

  @PostMapping("/{id}/equipment")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Add equipment to court", description = "Adds equipment to a court")
  public ResponseEntity<CourtResponseDTO> addEquipment(
      @PathVariable Long id,
      @RequestBody List<Long> equipmentIds) {

    CourtResponseDTO updatedCourt = courtService.addEquipment(id, equipmentIds);
    return ResponseEntity.ok(updatedCourt);
  }

  @DeleteMapping("/{id}/equipment")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Remove equipment from court", description = "Removes equipment from a court")
  public ResponseEntity<CourtResponseDTO> removeEquipment(
      @PathVariable Long id,
      @RequestBody List<Long> equipmentIds) {

    CourtResponseDTO updatedCourt = courtService.removeEquipment(id, equipmentIds);
    return ResponseEntity.ok(updatedCourt);
  }

  @GetMapping("/{id}/analytics")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get court analytics", description = "Gets analytics data for the court")
  public ResponseEntity<Map<String, Object>> getCourtAnalytics(
      @PathVariable Long id,
      @RequestParam(required = false) String period) {

    Map<String, Object> analytics = courtService.getCourtAnalytics(id, period);
    return ResponseEntity.ok(analytics);
  }

  @GetMapping("/{id}/occupancy")
  @Operation(summary = "Get court occupancy rate", description = "Gets the occupancy rate for a court")
  public ResponseEntity<Map<String, Object>> getCourtOccupancy(
      @PathVariable Long id,
      @RequestParam(required = false) String period) {

    Map<String, Object> occupancy = courtService.getCourtOccupancy(id, period);
    return ResponseEntity.ok(occupancy);
  }

  @GetMapping("/{id}/bookings")
  @Operation(summary = "Get court bookings", description = "Gets all bookings for a court")
  public ResponseEntity<Map<String, Object>> getCourtBookings(
      @PathVariable Long id,
      @RequestParam(required = false) String date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Map<String, Object> bookings = courtService.getCourtBookings(
        id, date != null ? LocalDate.parse(date) : null, page, size);
    return ResponseEntity.ok(bookings);
  }

  @GetMapping("/popular")
  @Operation(summary = "Get popular courts", description = "Gets the most popular courts based on bookings")
  public ResponseEntity<List<CourtResponseDTO>> getPopularCourts(
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(required = false) Long venueId) {

    List<CourtResponseDTO> courts = courtService.getPopularCourts(limit, venueId);
    return ResponseEntity.ok(courts);
  }

  @GetMapping("/recommendations")
  @Operation(summary = "Get court recommendations", description = "Gets personalized court recommendations")
  public ResponseEntity<List<CourtResponseDTO>> getCourtRecommendations(
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) String sportType,
      @RequestParam(required = false) String location,
      @RequestParam(defaultValue = "10") int limit) {

    List<CourtResponseDTO> courts = courtService.getCourtRecommendations(
        customerId, sportType, location, limit);
    return ResponseEntity.ok(courts);
  }
}