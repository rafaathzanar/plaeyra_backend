package com.zanar.playera.controller;

import com.zanar.playera.dto.AnalyticsResponseDTO;
import com.zanar.playera.service.AnalyticsService;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.repo.BookingRepository;
import com.zanar.playera.repo.VenueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

  @Autowired
  private AnalyticsService analyticsService;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private VenueRepository venueRepository;

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

  @GetMapping("/venue/{venueId}/export/monthly")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Export monthly revenue report", description = "Exports comprehensive monthly revenue report in CSV format")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CSV report generated successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<String> exportMonthlyRevenueReport(@PathVariable Long venueId) {
    try {
      AnalyticsResponseDTO analytics = analyticsService.getVenueAnalytics(venueId, "month");
      String csvContent = generateRevenueCSV(analytics, "Monthly", venueId);

      String fileName = String.format("monthly_revenue_report_%s_%s.csv",
          venueId, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
      headers.setContentDispositionFormData("attachment", fileName);
      headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
      headers.add("Pragma", "no-cache");
      headers.add("Expires", "0");

      return ResponseEntity.ok()
          .headers(headers)
          .body(csvContent);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/venue/{venueId}/export/weekly")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Export weekly revenue report", description = "Exports comprehensive weekly revenue report in CSV format")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "CSV report generated successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Venue not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<String> exportWeeklyRevenueReport(@PathVariable Long venueId) {
    try {
      AnalyticsResponseDTO analytics = analyticsService.getVenueAnalytics(venueId, "week");
      String csvContent = generateRevenueCSV(analytics, "Weekly", venueId);

      String fileName = String.format("weekly_revenue_report_%s_%s.csv",
          venueId, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
      headers.setContentDispositionFormData("attachment", fileName);
      headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
      headers.add("Pragma", "no-cache");
      headers.add("Expires", "0");

      return ResponseEntity.ok()
          .headers(headers)
          .body(csvContent);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/venue/{venueId}/debug")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Debug venue analytics", description = "Debug endpoint to see raw analytics data")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> debugVenueAnalytics(@PathVariable Long venueId) {
    try {
      Map<String, Object> debugInfo = new HashMap<>();

      // Get basic venue info
      Venue venue = venueRepository.findById(venueId).orElse(null);
      if (venue != null) {
        debugInfo.put("venueId", venue.getVenueId());
        debugInfo.put("venueName", venue.getName());
      }

      // Get all bookings
      List<Booking> allBookings = bookingRepository.findByVenueIdWithDetails(venueId);
      debugInfo.put("totalBookings", allBookings.size());

      // Get booking details
      List<Map<String, Object>> bookingDetails = new ArrayList<>();
      for (Booking booking : allBookings) {
        Map<String, Object> bookingInfo = new HashMap<>();
        bookingInfo.put("bookingId", booking.getBookingId());
        bookingInfo.put("bookingStatus", booking.getBookingStatus());
        bookingInfo.put("totalCost", booking.getTotalCost());
        bookingInfo.put("bookingDate", booking.getBookingDate());
        bookingInfo.put("customerId", booking.getCustomer() != null ? booking.getCustomer().getUserId() : null);
        bookingInfo.put("isBooked", "BOOKED".equals(booking.getBookingStatus()));
        bookingDetails.add(bookingInfo);
      }
      debugInfo.put("bookings", bookingDetails);

      // Count by status
      Map<String, Long> statusCounts = allBookings.stream()
          .collect(Collectors.groupingBy(
              b -> b.getBookingStatus() != null ? b.getBookingStatus().toString() : "NULL",
              Collectors.counting()));
      debugInfo.put("statusCounts", statusCounts);

      return ResponseEntity.ok(debugInfo);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  private String generateRevenueCSV(AnalyticsResponseDTO analytics, String reportType, Long venueId) {
    StringBuilder csv = new StringBuilder();

    // Add BOM for proper UTF-8 encoding
    csv.append("\uFEFF");

    // CSV Header
    csv.append("Revenue Report - ").append(reportType).append("\n");
    csv.append("Generated on: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .append("\n\n");

    // Summary Section
    csv.append("SUMMARY\n");
    csv.append("Metric,Value\n");
    csv.append("Total Revenue,LKR ").append(String.format("%.2f", analytics.getTotalRevenue())).append("\n");
    csv.append("Court Revenue,LKR ").append(String.format("%.2f", analytics.getCourtRevenue())).append("\n");
    csv.append("Equipment Revenue,LKR ").append(String.format("%.2f", analytics.getEquipmentRevenue())).append("\n");
    csv.append("Total Bookings,").append(analytics.getTotalBookings()).append("\n");
    csv.append("Confirmed Bookings,").append(analytics.getConfirmedBookings()).append("\n");
    csv.append("Cancelled Bookings,").append(analytics.getCancelledBookings()).append("\n");
    csv.append("Total Customers,").append(analytics.getTotalCustomers()).append("\n");
    csv.append("New Customers,").append(analytics.getNewCustomers()).append("\n");
    csv.append("Returning Customers,").append(analytics.getReturningCustomers()).append("\n\n");

    // Court Performance Section
    if (analytics.getCourtRevenueMap() != null && !analytics.getCourtRevenueMap().isEmpty()) {
      csv.append("COURT PERFORMANCE\n");
      csv.append("Court Name,Bookings,Revenue,Occupancy %\n");

      Map<Long, String> courtNames = analytics.getCourtNames() != null ? analytics.getCourtNames() : new HashMap<>();
      Map<Long, Integer> courtBookings = analytics.getCourtBookings() != null ? analytics.getCourtBookings()
          : new HashMap<>();
      Map<Long, Double> courtOccupancy = analytics.getCourtOccupancy() != null ? analytics.getCourtOccupancy()
          : new HashMap<>();

      for (Map.Entry<Long, Double> entry : analytics.getCourtRevenueMap().entrySet()) {
        Long courtId = entry.getKey();
        String courtName = courtNames.getOrDefault(courtId, "Court " + courtId);
        Integer bookings = courtBookings.getOrDefault(courtId, 0);
        Double occupancy = courtOccupancy.getOrDefault(courtId, 0.0);

        csv.append("\"").append(courtName).append("\",")
            .append(bookings).append(",")
            .append("LKR ").append(String.format("%.2f", entry.getValue())).append(",")
            .append(String.format("%.1f", occupancy)).append("\n");
      }
      csv.append("\n");
    }

    // Equipment Performance Section
    if (analytics.getEquipmentRevenueMap() != null && !analytics.getEquipmentRevenueMap().isEmpty()) {
      csv.append("EQUIPMENT PERFORMANCE\n");
      csv.append("Equipment Name,Usage Count,Revenue\n");

      Map<Long, String> equipmentNames = analytics.getEquipmentNames() != null ? analytics.getEquipmentNames()
          : new HashMap<>();
      Map<Long, Integer> equipmentUsage = analytics.getEquipmentUsage() != null ? analytics.getEquipmentUsage()
          : new HashMap<>();

      for (Map.Entry<Long, Double> entry : analytics.getEquipmentRevenueMap().entrySet()) {
        Long equipmentId = entry.getKey();
        String equipmentName = equipmentNames.getOrDefault(equipmentId, "Equipment " + equipmentId);
        Integer usage = equipmentUsage.getOrDefault(equipmentId, 0);

        csv.append("\"").append(equipmentName).append("\",")
            .append(usage).append(",")
            .append("LKR ").append(String.format("%.2f", entry.getValue())).append("\n");
      }
      csv.append("\n");
    }

    // Monthly Trends Section
    if (analytics.getMonthlyTrends() != null && !analytics.getMonthlyTrends().isEmpty()) {
      csv.append("MONTHLY TRENDS\n");
      csv.append("Month,Revenue\n");

      for (Map.Entry<String, Double> entry : analytics.getMonthlyTrends().entrySet()) {
        csv.append("\"").append(entry.getKey()).append("\",")
            .append("LKR ").append(String.format("%.2f", entry.getValue())).append("\n");
      }
      csv.append("\n");
    }

    // Peak Hours Section
    if (analytics.getPeakHours() != null && !analytics.getPeakHours().isEmpty()) {
      csv.append("PEAK HOURS\n");
      csv.append("Time Slot\n");

      for (String peakHour : analytics.getPeakHours()) {
        csv.append("\"").append(peakHour).append("\"\n");
      }
      csv.append("\n");
    }

    // Off-Peak Hours Section
    if (analytics.getOffPeakHours() != null && !analytics.getOffPeakHours().isEmpty()) {
      csv.append("OFF-PEAK HOURS\n");
      csv.append("Time Slot\n");

      for (String offPeakHour : analytics.getOffPeakHours()) {
        csv.append("\"").append(offPeakHour).append("\"\n");
      }
      csv.append("\n");
    }

    // Booking Details Section
    csv.append("BOOKING DETAILS\n");
    csv.append(
        "Booking ID,Customer Name,Customer Email,Court Name,Booking Date,Booking Time,Status,Total Amount,Payment Status,Equipment Count,Equipment Details\n");

    // Get detailed booking information
    List<Booking> allBookings = bookingRepository.findByVenueIdWithDetails(venueId);

    for (Booking booking : allBookings) {
      // Basic booking info
      String bookingId = booking.getBookingId().toString();
      String customerName = booking.getCustomer() != null ? booking.getCustomer().getName() : "Unknown";
      String customerEmail = booking.getCustomer() != null ? booking.getCustomer().getEmail() : "Unknown";

      // Court information
      String courtName = "Unknown";
      if (booking.getBookingCourts() != null && !booking.getBookingCourts().isEmpty()) {
        courtName = booking.getBookingCourts().get(0).getCourt().getCourtName();
      }

      // Date and time
      String bookingDate = booking.getBookingDate() != null ? booking.getBookingDate().toLocalDate().toString()
          : "Unknown";
      String bookingTime = booking.getBookingDate() != null ? booking.getBookingDate().toLocalTime().toString()
          : "Unknown";

      // Status and amount
      String status = booking.getBookingStatus() != null ? booking.getBookingStatus().toString() : "Unknown";
      String totalAmount = String.format("%.2f", booking.getTotalCost());

      // Payment status
      String paymentStatus = "Unknown";
      if (booking.getPayment() != null) {
        paymentStatus = booking.getPayment().getStatus() != null ? booking.getPayment().getStatus().toString()
            : "Unknown";
      }

      // Equipment information
      int equipmentCount = booking.getBookingEquipments() != null ? booking.getBookingEquipments().size() : 0;
      StringBuilder equipmentDetails = new StringBuilder();
      if (booking.getBookingEquipments() != null && !booking.getBookingEquipments().isEmpty()) {
        for (int i = 0; i < booking.getBookingEquipments().size(); i++) {
          var be = booking.getBookingEquipments().get(i);
          if (i > 0)
            equipmentDetails.append("; ");
          equipmentDetails.append(be.getEquipment() != null ? be.getEquipment().getName() : "Unknown")
              .append(" (LKR ").append(String.format("%.2f", be.getTotalPrice())).append(")");
        }
      }

      // Write booking row
      csv.append("\"").append(bookingId).append("\",")
          .append("\"").append(customerName).append("\",")
          .append("\"").append(customerEmail).append("\",")
          .append("\"").append(courtName).append("\",")
          .append("\"").append(bookingDate).append("\",")
          .append("\"").append(bookingTime).append("\",")
          .append("\"").append(status).append("\",")
          .append("LKR ").append(totalAmount).append(",")
          .append("\"").append(paymentStatus).append("\",")
          .append(equipmentCount).append(",")
          .append("\"").append(equipmentDetails.toString()).append("\"\n");
    }

    return csv.toString();
  }

  @GetMapping("/venue/{venueId}/all-bookings")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Get all bookings for venue", description = "Get all bookings for a venue regardless of date range.")
  public ResponseEntity<Map<String, Object>> getAllBookingsForVenue(@PathVariable Long venueId) {
    List<Booking> allBookings = bookingRepository.findByVenueIdWithDetails(venueId);

    Map<String, Object> result = new HashMap<>();
    result.put("totalBookings", allBookings.size());

    // Count by status
    Map<String, Long> statusCounts = allBookings.stream()
        .collect(Collectors.groupingBy(
            b -> b.getBookingStatus() != null ? b.getBookingStatus().toString() : "NULL",
            Collectors.counting()));
    result.put("statusCounts", statusCounts);

    // Count cancelled bookings specifically
    long cancelledCount = allBookings.stream()
        .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
        .count();
    result.put("cancelledBookings", cancelledCount);

    return ResponseEntity.ok(result);
  }
}
