package com.zanar.playera.controller;

import com.zanar.playera.dto.UserLoginDTO;
import com.zanar.playera.dto.UserRegistrationDTO;
import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  private UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserResponseDTO> register(@RequestBody UserRegistrationDTO dto) {
    UserResponseDTO response = userService.registerUser(dto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponseDTO> login(@RequestBody UserLoginDTO dto) {
    UserResponseDTO response = userService.loginUser(dto);
    return ResponseEntity.ok(response);
  }
}