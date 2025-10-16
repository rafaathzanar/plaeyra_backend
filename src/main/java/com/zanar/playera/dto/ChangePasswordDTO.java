package com.zanar.playera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
  @NotBlank(message = "Current password is required")
  private String currentPassword;

  @NotBlank(message = "New password is required")
  @Size(min = 6, message = "New password must be at least 6 characters")
  private String newPassword;

  @NotBlank(message = "Confirm password is required")
  private String confirmPassword;
}
