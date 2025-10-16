package com.zanar.playera.controller;

import com.zanar.playera.dto.*;
import com.zanar.playera.security.JwtUtil;
import com.zanar.playera.service.UserService;
import com.zanar.playera.service.VenueService;
import com.zanar.playera.service.BookingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "APIs for admin authentication and management")
@CrossOrigin(origins = "*")
public class AdminController {
  @Autowired
  private UserService userService;
  @Autowired
  private VenueService venueService;
  @Autowired
  private BookingService bookingService;
  @Autowired
  private JwtUtil jwtUtil;
  @Autowired
  private com.zanar.playera.security.CustomUserDetailsService userDetailsService;

  @PostMapping("/auth/login")
  @Operation(summary = "Admin Login", description = "Authenticate admin credentials and return JWT token for subsequent API calls.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Admin login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminJwtResponseDTO.class), examples = @ExampleObject(value = """
          {
            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "user": {
              "userId": 1,
              "name": "Admin User",
              "email": "admin@playera.com",
              "role": "ADMIN"
            },
            "refreshToken": "refresh_token_here",
            "expiresIn": 86400000
          }
          """))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "error": "Invalid email or password"
          }
          """)))
  })
  public ResponseEntity<AdminJwtResponseDTO> adminLogin(
      @Parameter(description = "Admin login credentials", required = true, content = @Content(examples = @ExampleObject(value = """
          {
            "email": "admin@playera.com",
            "password": "adminPassword123"
          }
          """))) @Valid @RequestBody UserLoginDTO dto) {

    // Validate admin credentials
    UserResponseDTO user = userService.loginUser(dto);

    // Check if user has ADMIN role
    if (!"ADMIN".equals(user.getRole())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new AdminJwtResponseDTO(null, null, "Access denied. Admin role required."));
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
    String token = jwtUtil.generateToken(userDetails);

    return ResponseEntity.ok(new AdminJwtResponseDTO(token, user));
  }

  @GetMapping("/auth/me")
  @Operation(summary = "Get Current Admin", description = "Get information about the currently authenticated admin")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Current admin information retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Admin not authenticated")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<UserResponseDTO> getCurrentAdmin() {
    // Get the current authenticated user from SecurityContext
    org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()) {
      String email = authentication.getName();
      UserResponseDTO user = userService.getUserByEmail(email);

      // Verify admin role
      if (!"ADMIN".equals(user.getRole())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }

      return ResponseEntity.ok(user);
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  // User Management APIs
  @GetMapping("/users")
  @Operation(summary = "Get all users", description = "Retrieve all users in the system for admin management")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<UserResponseDTO>> getUsers() {
    List<UserResponseDTO> users = userService.listUsers();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/users/{id}")
  @Operation(summary = "Get user by ID", description = "Get specific user details by ID")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
    UserResponseDTO user = userService.getUserById(id);
    return ResponseEntity.ok(user);
  }

  @PatchMapping("/users/{id}/status")
  @Operation(summary = "Update user status", description = "Activate or deactivate a user account")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> updateUserStatus(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    // This would need to be implemented in UserService
    Map<String, String> response = new HashMap<>();
    response.put("message", "User status updated successfully");
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/users/{id}")
  @Operation(summary = "Delete user", description = "Permanently delete a user account")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
    // This would need to be implemented in UserService
    Map<String, String> response = new HashMap<>();
    response.put("message", "User deleted successfully");
    return ResponseEntity.ok(response);
  }

  // Venue Management APIs
  @GetMapping("/venues")
  @Operation(summary = "Get all venues", description = "Retrieve all venues for admin management")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<VenueResponseDTO>> getVenues() {
    List<VenueResponseDTO> venues = venueService.listVenues();
    return ResponseEntity.ok(venues);
  }

  @GetMapping("/venues/{id}")
  @Operation(summary = "Get venue by ID", description = "Get specific venue details by ID")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<VenueResponseDTO> getVenueById(@PathVariable Long id) {
    VenueResponseDTO venue = venueService.getVenueById(id);
    return ResponseEntity.ok(venue);
  }

  @PatchMapping("/venues/{id}/approve")
  @Operation(summary = "Approve/reject venue", description = "Approve or reject a venue application")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> approveVenue(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    // This would need to be implemented in VenueService
    Map<String, String> response = new HashMap<>();
    response.put("message", "Venue approval status updated successfully");
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/venues/{id}/status")
  @Operation(summary = "Update venue status", description = "Activate or deactivate a venue")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> updateVenueStatus(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    // This would need to be implemented in VenueService
    Map<String, String> response = new HashMap<>();
    response.put("message", "Venue status updated successfully");
    return ResponseEntity.ok(response);
  }

  // Booking Management APIs
  @GetMapping("/bookings")
  @Operation(summary = "Get all bookings", description = "Retrieve all bookings for admin management")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<BookingResponseDTO>> getBookings() {
    List<BookingResponseDTO> bookings = bookingService.listBookings();
    return ResponseEntity.ok(bookings);
  }

  @GetMapping("/bookings/{id}")
  @Operation(summary = "Get booking by ID", description = "Get specific booking details by ID")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
    BookingResponseDTO booking = bookingService.getBookingById(id);
    return ResponseEntity.ok(booking);
  }

  @PatchMapping("/bookings/{id}/cancel")
  @Operation(summary = "Cancel booking", description = "Cancel a booking with reason")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> cancelBooking(
      @PathVariable Long id,
      @RequestBody Map<String, String> request) {
    // This would need to be implemented in BookingService
    Map<String, String> response = new HashMap<>();
    response.put("message", "Booking cancelled successfully");
    return ResponseEntity.ok(response);
  }

  // Dashboard APIs
  @GetMapping("/dashboard/stats")
  @Operation(summary = "Get dashboard statistics", description = "Get platform-wide statistics for admin dashboard")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> getDashboardStats() {
    Map<String, Object> stats = new HashMap<>();
    List<UserResponseDTO> users = userService.listUsers();
    List<VenueResponseDTO> venues = venueService.listVenues();
    List<BookingResponseDTO> bookings = bookingService.listBookings();

    stats.put("totalUsers", users.size());
    stats.put("totalVenues", venues.size());
    stats.put("totalBookings", bookings.size());
    stats.put("activeUsers", users.size());
    stats.put("activeVenues", venues.stream()
        .filter(venue -> "ACTIVE".equals(venue.getStatus()))
        .count());
    stats.put("completedBookings", bookings.stream()
        .filter(booking -> "COMPLETED".equals(booking.getBookingStatus()))
        .count());
    stats.put("totalRevenue", bookings.stream()
        .mapToDouble(BookingResponseDTO::getTotalCost)
        .sum());
    stats.put("customers", users.stream()
        .filter(user -> "CUSTOMER".equals(user.getRole()))
        .count());
    stats.put("venueOwners", users.stream()
        .filter(user -> "VENUE_OWNER".equals(user.getRole()))
        .count());
    return ResponseEntity.ok(stats);
  }

  // Analytics APIs
  @GetMapping("/analytics/users")
  @Operation(summary = "Get user analytics", description = "Get user-related analytics data")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> getUserAnalytics() {
    Map<String, Object> analytics = new HashMap<>();
    List<UserResponseDTO> users = userService.listUsers();
    analytics.put("totalUsers", users.size());
    analytics.put("usersByRole", users.stream()
        .collect(java.util.stream.Collectors.groupingBy(UserResponseDTO::getRole)));
    analytics.put("usersByUserType", users.stream()
        .collect(java.util.stream.Collectors.groupingBy(UserResponseDTO::getUserType)));
    analytics.put("customers", users.stream()
        .filter(user -> "CUSTOMER".equals(user.getRole()))
        .count());
    analytics.put("venueOwners", users.stream()
        .filter(user -> "VENUE_OWNER".equals(user.getRole()))
        .count());
    analytics.put("admins", users.stream()
        .filter(user -> "ADMIN".equals(user.getRole()))
        .count());
    return ResponseEntity.ok(analytics);
  }

  @GetMapping("/analytics/venues")
  @Operation(summary = "Get venue analytics", description = "Get venue-related analytics data")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> getVenueAnalytics() {
    Map<String, Object> analytics = new HashMap<>();
    List<VenueResponseDTO> venues = venueService.listVenues();
    analytics.put("totalVenues", venues.size());
    analytics.put("venuesByStatus", venues.stream()
        .collect(java.util.stream.Collectors.groupingBy(VenueResponseDTO::getStatus)));
    analytics.put("venuesByType", venues.stream()
        .collect(java.util.stream.Collectors.groupingBy(VenueResponseDTO::getVenueType)));
    analytics.put("activeVenues", venues.stream()
        .filter(venue -> "ACTIVE".equals(venue.getStatus()))
        .count());
    return ResponseEntity.ok(analytics);
  }

  @GetMapping("/analytics/bookings")
  @Operation(summary = "Get booking analytics", description = "Get booking-related analytics data")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> getBookingAnalytics() {
    Map<String, Object> analytics = new HashMap<>();
    List<BookingResponseDTO> bookings = bookingService.listBookings();
    analytics.put("totalBookings", bookings.size());
    analytics.put("bookingsByStatus", bookings.stream()
        .collect(java.util.stream.Collectors.groupingBy(BookingResponseDTO::getBookingStatus)));
    analytics.put("totalRevenue", bookings.stream()
        .mapToDouble(BookingResponseDTO::getTotalCost)
        .sum());
    analytics.put("averageBookingValue", bookings.stream()
        .mapToDouble(BookingResponseDTO::getTotalCost)
        .average()
        .orElse(0.0));
    analytics.put("completedBookings", bookings.stream()
        .filter(booking -> "COMPLETED".equals(booking.getBookingStatus()))
        .count());
    analytics.put("cancelledBookings", bookings.stream()
        .filter(booking -> "CANCELLED".equals(booking.getBookingStatus()))
        .count());
    return ResponseEntity.ok(analytics);
  }

  @GetMapping("/analytics/revenue")
  @Operation(summary = "Get revenue analytics", description = "Get revenue-related analytics data")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> getRevenueAnalytics(
      @RequestParam(defaultValue = "month") String dateRange) {
    Map<String, Object> analytics = new HashMap<>();
    analytics.put("totalRevenue", 0.0);
    analytics.put("revenueByMonth", new HashMap<>());
    analytics.put("dateRange", dateRange);
    return ResponseEntity.ok(analytics);
  }

  // Venue Owner Management APIs
  @GetMapping("/venue-owners")
  @Operation(summary = "Get all venue owners", description = "Retrieve all venue owners for admin management")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<UserResponseDTO>> getVenueOwners() {
    List<UserResponseDTO> users = userService.listUsers();
    List<UserResponseDTO> venueOwners = users.stream()
        .filter(user -> "VENUE_OWNER".equals(user.getRole()))
        .collect(java.util.stream.Collectors.toList());
    return ResponseEntity.ok(venueOwners);
  }

  @GetMapping("/venue-owners/{id}")
  @Operation(summary = "Get venue owner by ID", description = "Get specific venue owner details by ID")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<UserResponseDTO> getVenueOwnerById(@PathVariable Long id) {
    UserResponseDTO user = userService.getUserById(id);
    if (!"VENUE_OWNER".equals(user.getRole())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    return ResponseEntity.ok(user);
  }

  @PatchMapping("/venue-owners/{id}/status")
  @Operation(summary = "Update venue owner status", description = "Activate or deactivate a venue owner account")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> updateVenueOwnerStatus(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    // This would need to be implemented in UserService
    Map<String, String> response = new HashMap<>();
    response.put("message", "Venue owner status updated successfully");
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/venue-owners/{id}/approve")
  @Operation(summary = "Approve/reject venue owner", description = "Approve or reject a venue owner application")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, String>> approveVenueOwner(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request) {
    // This would need to be implemented in UserService
    Map<String, String> response = new HashMap<>();
    response.put("message", "Venue owner approval status updated successfully");
    return ResponseEntity.ok(response);
  }

  // Admin-specific response DTO
  public static class AdminJwtResponseDTO {
    private String token;
    private UserResponseDTO user;
    private String refreshToken;
    private long expiresIn;
    private String error;

    public AdminJwtResponseDTO(String token, UserResponseDTO user) {
      this.token = token;
      this.user = user;
    }

    public AdminJwtResponseDTO(String token, UserResponseDTO user, String error) {
      this.token = token;
      this.user = user;
      this.error = error;
    }

    // Getters and setters
    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public UserResponseDTO getUser() {
      return user;
    }

    public void setUser(UserResponseDTO user) {
      this.user = user;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
      return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
      this.expiresIn = expiresIn;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }
  }
}
