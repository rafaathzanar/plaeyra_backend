package com.zanar.playera.controller;

import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.dto.UserUpdateDTO;
import com.zanar.playera.dto.ChangePasswordDTO;
import com.zanar.playera.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
public class UserController {
  @Autowired
  private UserService userService;

  @GetMapping
  public ResponseEntity<List<UserResponseDTO>> listUsers() {
    return ResponseEntity.ok(userService.listUsers());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @GetMapping("/email/{email}")
  public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
    return ResponseEntity.ok(userService.getUserByEmail(email));
  }

  @GetMapping("/profile")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<UserResponseDTO> getCurrentUserProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    return ResponseEntity.ok(userService.getUserByEmail(email));
  }

  @PutMapping("/profile")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<UserResponseDTO> updateProfile(@RequestBody UserUpdateDTO updateDTO) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    return ResponseEntity.ok(userService.updateUserProfile(email, updateDTO));
  }

  @PostMapping("/change-password")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();

    try {
      userService.changePassword(email, changePasswordDTO);
      Map<String, String> response = new HashMap<>();
      response.put("message", "Password changed successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> response = new HashMap<>();
      response.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

  @DeleteMapping("/profile")
  @PreAuthorize("hasRole('CUSTOMER') or hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> deleteAccount() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();

    try {
      userService.deleteUserAccount(email);
      Map<String, String> response = new HashMap<>();
      response.put("message", "Account deleted successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> response = new HashMap<>();
      response.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }
}
