package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class PasswordResetDTO {
    @NotBlank(message = "Reset token is required")
    private String token;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
} 