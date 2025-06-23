package com.zanar.playera.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
  private Long userId;
  private String name;
  private String email;
  private String phone;
  private String userType;
  private Integer loyaltyPoints;
}