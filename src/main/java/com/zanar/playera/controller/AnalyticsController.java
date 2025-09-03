package com.zanar.playera.controller;

import com.zanar.playera.dto.AnalyticsResponseDTO;
import com.zanar.playera.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

  @Autowired
  private AnalyticsService analyticsService;

  @GetMapping("/venue/{venueId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue analytics", description = "Retrieves comprehensive analytics data for a specific venue including revenue, bookings, occupancy, and performance metrics.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Analytics data retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<AnalyticsResponseDTO> getVenueAnalytics(
      @PathVariable Long venueId,
      @RequestParam(defaultValue = "month") String dateRange) {

    try {
      AnalyticsResponseDTO analytics = analyticsService.getVenueAnalytics(venueId, dateRange);
      return ResponseEntity.ok(analytics);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/venue/{venueId}/revenue")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue revenue analytics", description = "Retrieves detailed revenue analytics for a specific venue.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Revenue analytics retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<AnalyticsResponseDTO> getVenueRevenueAnalytics(
      @PathVariable Long venueId,
      @RequestParam(defaultValue = "month") String dateRange) {

    try {
      AnalyticsResponseDTO analytics = analyticsService.getVenueAnalytics(venueId, dateRange);
      return ResponseEntity.ok(analytics);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/venue/{venueId}/occupancy")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get venue occupancy analytics", description = "Retrieves court occupancy and utilization analytics for a specific venue.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Occupancy analytics retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<AnalyticsResponseDTO> getVenueOccupancyAnalytics(
      @PathVariable Long venueId,
      @RequestParam(defaultValue = "month") String dateRange) {

    try {
      AnalyticsResponseDTO analytics = analyticsService.getVenueAnalytics(venueId, dateRange);
      return ResponseEntity.ok(analytics);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
