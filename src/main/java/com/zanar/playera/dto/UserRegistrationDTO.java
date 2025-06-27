package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UserRegistrationDTO {
  @NotBlank(message = "Name is required")
  private String name;
  @NotBlank(message = "Email is required")
  private String email;
  @NotBlank(message = "Password is required")
  private String password;
  @NotBlank(message = "Phone is required")
  private String phone;
  @NotBlank(message = "Role is required")
  private String role; // CUSTOMER, VENUE_OWNER, ADMIN
  private String userType; // CUSTOMER or VENUE_OWNER
  private Integer loyaltyPoints; // Optional, for CUSTOMER
}