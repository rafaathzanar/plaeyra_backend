package com.zanar.playera.dto;

import lombok.Data;

@Data
public class UserLoginDTO {
  private String email;
  private String password;
}