package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EquipmentResponseDTO {
    private Long equipmentId;
    private String name;
    private String description;
    private Double ratePerHour;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer rentedQuantity;
    private String status;
    private Integer minimumRentalHours;
    private Integer maximumRentalHours;
    private Long courtId;
    private String courtName;
    private LocalDateTime lastMaintenanceDate;
    private Boolean isAvailable;

    // Calculated fields
    private Double estimatedCostPerHour;
}