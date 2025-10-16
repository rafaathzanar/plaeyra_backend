package com.zanar.playera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  private String name;

  @Email(message = "Email should be valid")
  private String email;

  @Size(max = 15, message = "Phone number must not exceed 15 characters")
  private String phone;

  private String profileImage;
}
