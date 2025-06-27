package com.zanar.playera.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class EquipmentRequestDTO {
    @NotBlank(message = "Equipment name is required")
    private String name;
    
    @NotBlank(message = "Equipment description is required")
    private String description;
    
    @NotNull(message = "Rate per hour is required")
    @Min(value = 0, message = "Rate per hour must be non-negative")
    private Double ratePerHour;
    
    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;
    
    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity must be non-negative")
    private Integer availableQuantity;
    
    @NotNull(message = "Deposit amount is required")
    @Min(value = 0, message = "Deposit amount must be non-negative")
    private Double depositAmount;
    
    @Min(value = 1, message = "Minimum rental hours must be at least 1")
    private Integer minimumRentalHours = 1;
    
    @Min(value = 1, message = "Maximum rental hours must be at least 1")
    private Integer maximumRentalHours = 24;
    
    @NotNull(message = "Court ID is required")
    private Long courtId;
    
    private LocalDateTime lastMaintenanceDate;
}