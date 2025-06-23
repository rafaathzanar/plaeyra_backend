package com.zanar.playera.dto;

import lombok.Data;

@Data
public class UserRegistrationDTO {
  private String name;
  private String email;
  private String password;
  private String phone;
  private String userType; // CUSTOMER or VENUE_OWNER
  private Integer loyaltyPoints; // Optional, for CUSTOMER
}