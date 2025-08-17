package com.zanar.playera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {
  private String token;
  private UserResponseDTO user;
  private String refreshToken;
  private long expiresIn;
}
