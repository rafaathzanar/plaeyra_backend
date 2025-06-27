package com.zanar.playera.controller;

import com.zanar.playera.dto.*;
import com.zanar.playera.security.JwtUtil;
import com.zanar.playera.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  private UserService userService;
  @Autowired
  private JwtUtil jwtUtil;
  @Autowired
  private com.zanar.playera.security.CustomUserDetailsService userDetailsService;

  @PostMapping("/register")
  public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO dto) {
    UserResponseDTO response = userService.registerUser(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody UserLoginDTO dto) {
    UserResponseDTO user = userService.loginUser(dto);
    UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
    String token = jwtUtil.generateToken(userDetails);
    return ResponseEntity.ok(new JwtResponseDTO(token, user));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody PasswordResetRequestDTO dto) {
    userService.requestPasswordReset(dto.getEmail());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
    userService.resetPassword(dto.getToken(), dto.getNewPassword());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    // For JWT, logout is handled client-side. This endpoint is for completeness.
    return ResponseEntity.ok().build();
  }

  public static class JwtResponseDTO {
    private String token;
    private UserResponseDTO user;

    public JwtResponseDTO(String token, UserResponseDTO user) {
      this.token = token;
      this.user = user;
    }

    public String getToken() {
      return token;
    }

    public UserResponseDTO getUser() {
      return user;
    }
  }
}