package com.zanar.playera.controller;

import com.zanar.playera.dto.*;
import com.zanar.playera.security.JwtUtil;
import com.zanar.playera.service.UserService;
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

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and password management")
@CrossOrigin(origins = "*")
public class AuthController {
  @Autowired
  private UserService userService;
  @Autowired
  private JwtUtil jwtUtil;
  @Autowired
  private com.zanar.playera.security.CustomUserDetailsService userDetailsService;

  @PostMapping("/register")
  @Operation(summary = "User Registration", description = "Register a new user account with the system. Supports different user roles (CUSTOMER, VENUE_OWNER, ADMIN).")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class), examples = @ExampleObject(name = "Customer Registration", value = """
          {
            "userId": 1,
            "name": "John Doe",
            "email": "john.doe@example.com",
            "phone": "+1234567890",
            "role": "CUSTOMER",
            "status": "ACTIVE"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid input data or email already exists", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "error": "Email already registered",
            "timestamp": "2024-01-15T10:30:00Z"
          }
          """)))
  })
  public ResponseEntity<UserResponseDTO> register(
      @Parameter(description = "User registration details", required = true, content = @Content(examples = @ExampleObject(name = "Customer Registration", value = """
          {
            "name": "John Doe",
            "email": "john.doe@example.com",
            "password": "securePassword123",
            "phone": "+1234567890",
            "role": "CUSTOMER",
            "userType": "CUSTOMER"
          }
          """))) @Valid @RequestBody UserRegistrationDTO dto) {
    UserResponseDTO response = userService.registerUser(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  @Operation(summary = "User Login", description = "Authenticate user credentials and return JWT token for subsequent API calls.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponseDTO.class), examples = @ExampleObject(value = """
          {
            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "user": {
              "userId": 1,
              "name": "John Doe",
              "email": "john.doe@example.com",
              "role": "CUSTOMER"
            },
            "refreshToken": "refresh_token_here",
            "expiresIn": 86400000
          }
          """))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
          {
            "error": "Invalid email or password",
            "timestamp": "2024-01-15T10:30:00Z"
          }
          """)))
  })
  public ResponseEntity<JwtResponseDTO> login(
      @Parameter(description = "User login credentials", required = true, content = @Content(examples = @ExampleObject(value = """
          {
            "email": "john.doe@example.com",
            "password": "securePassword123"
          }
          """))) @Valid @RequestBody UserLoginDTO dto) {
    UserResponseDTO user = userService.loginUser(dto);
    UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
    String token = jwtUtil.generateToken(userDetails);
    return ResponseEntity.ok(new JwtResponseDTO(token, user));
  }

  @PostMapping("/forgot-password")
  @Operation(summary = "Forgot Password", description = "Request a password reset link to be sent to the user's email address.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Password reset email sent successfully"),
      @ApiResponse(responseCode = "404", description = "Email not found in the system")
  })
  public ResponseEntity<Void> forgotPassword(
      @Parameter(description = "Password reset request details", required = true) @Valid @RequestBody PasswordResetRequestDTO dto) {
    userService.requestPasswordReset(dto.getEmail());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Reset Password", description = "Reset user password using the token received via email.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Password reset successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired token")
  })
  public ResponseEntity<Void> resetPassword(
      @Parameter(description = "Password reset details with token and new password", required = true) @Valid @RequestBody PasswordResetDTO dto) {
    userService.resetPassword(dto.getToken(), dto.getNewPassword());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/logout")
  @Operation(summary = "User Logout", description = "Logout user (JWT tokens are invalidated client-side). This endpoint is for completeness.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Logout successful")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> logout() {
    // For JWT, logout is handled client-side. This endpoint is for completeness.
    return ResponseEntity.ok().build();
  }

  public static class JwtResponseDTO {
    private String token;
    private UserResponseDTO user;
    private String refreshToken;
    private long expiresIn;

    public JwtResponseDTO(String token, UserResponseDTO user) {
      this.token = token;
      this.user = user;
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
  }
}