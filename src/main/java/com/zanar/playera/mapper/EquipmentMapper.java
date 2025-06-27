package com.zanar.playera.mapper;

import com.zanar.playera.dto.EquipmentRequestDTO;
import com.zanar.playera.dto.EquipmentResponseDTO;
import com.zanar.playera.entity.Equipment;
import com.zanar.playera.entity.Court;

public class EquipmentMapper {
  public static Equipment toEquipmentEntity(EquipmentRequestDTO dto, Court court) {
    Equipment equipment = new Equipment();
    equipment.setName(dto.getName());
    equipment.setDescription(dto.getDescription());
    equipment.setRatePerHour(dto.getRatePerHour());
    equipment.setTotalQuantity(dto.getTotalQuantity());
    equipment.setAvailableQuantity(dto.getAvailableQuantity());
    equipment.setDepositAmount(dto.getDepositAmount());
    equipment.setMinimumRentalHours(dto.getMinimumRentalHours());
    equipment.setMaximumRentalHours(dto.getMaximumRentalHours());
    equipment.setLastMaintenanceDate(dto.getLastMaintenanceDate());
    equipment.setCourt(court);
    return equipment;
  }

  public static EquipmentResponseDTO toEquipmentResponseDTO(Equipment equipment) {
    EquipmentResponseDTO dto = new EquipmentResponseDTO();
    dto.setEquipmentId(equipment.getEquipmentId());
    dto.setName(equipment.getName());
    dto.setDescription(equipment.getDescription());
    dto.setRatePerHour(equipment.getRatePerHour());
    dto.setTotalQuantity(equipment.getTotalQuantity());
    dto.setAvailableQuantity(equipment.getAvailableQuantity());
    dto.setRentedQuantity(equipment.getRentedQuantity());
    dto.setStatus(equipment.getStatus().name());
    dto.setDepositAmount(equipment.getDepositAmount());
    dto.setMinimumRentalHours(equipment.getMinimumRentalHours());
    dto.setMaximumRentalHours(equipment.getMaximumRentalHours());
    dto.setLastMaintenanceDate(equipment.getLastMaintenanceDate());
    dto.setIsAvailable(equipment.isAvailable());
    
    if (equipment.getCourt() != null) {
      dto.setCourtId(equipment.getCourt().getCourtId());
      dto.setCourtName(equipment.getCourt().getCourtName());
    }
    
    // Calculated fields
    dto.setEstimatedCostPerHour(equipment.getRatePerHour());
    dto.setEstimatedDeposit(equipment.getDepositAmount());
    
    return dto;
  }
}