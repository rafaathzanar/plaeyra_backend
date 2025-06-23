package com.zanar.playera.controller;

import com.zanar.playera.dto.UserLoginDTO;
import com.zanar.playera.dto.UserRegistrationDTO;
import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.security.JwtUtil;
import com.zanar.playera.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<UserResponseDTO> register(@RequestBody UserRegistrationDTO dto) {
    UserResponseDTO response = userService.registerUser(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<JwtResponseDTO> login(@RequestBody UserLoginDTO dto) {
    UserResponseDTO user = userService.loginUser(dto);
    UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
    String token = jwtUtil.generateToken(userDetails);
    return ResponseEntity.ok(new JwtResponseDTO(token, user));
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