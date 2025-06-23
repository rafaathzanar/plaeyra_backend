package com.zanar.playera.mapper;

import com.zanar.playera.dto.EquipmentRequestDTO;
import com.zanar.playera.dto.EquipmentResponseDTO;
import com.zanar.playera.entity.Equipment;
import com.zanar.playera.entity.Court;

public class EquipmentMapper {
  public static Equipment toEquipmentEntity(EquipmentRequestDTO dto, Court court) {
    Equipment equipment = new Equipment();
    equipment.setName(dto.getName());
    equipment.setRatePerHour(dto.getRatePerHour());
    equipment.setAvailableQuantity(dto.getAvailableQuantity());
    equipment.setCourt(court);
    return equipment;
  }

  public static EquipmentResponseDTO toEquipmentResponseDTO(Equipment equipment) {
    EquipmentResponseDTO dto = new EquipmentResponseDTO();
    dto.setEquipmentId(equipment.getEquipmentId());
    dto.setName(equipment.getName());
    dto.setRatePerHour(equipment.getRatePerHour());
    dto.setAvailableQuantity(equipment.getAvailableQuantity());
    if (equipment.getCourt() != null) {
      dto.setCourtId(equipment.getCourt().getCourtId());
      dto.setCourtName(equipment.getCourt().getCourtName());
    }
    return dto;
  }
}