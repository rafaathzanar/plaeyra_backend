package com.zanar.playera.service;

import com.zanar.playera.dto.EquipmentRequestDTO;
import com.zanar.playera.dto.EquipmentResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Equipment;
import com.zanar.playera.mapper.EquipmentMapper;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentService {
  @Autowired
  private EquipmentRepository equipmentRepository;
  @Autowired
  private CourtRepository courtRepository;

  public EquipmentResponseDTO createEquipment(EquipmentRequestDTO dto) {
    Court court = courtRepository.findById(dto.getCourtId())
        .orElseThrow(() -> new RuntimeException("Court not found"));
    Equipment equipment = EquipmentMapper.toEquipmentEntity(dto, court);
    Equipment saved = equipmentRepository.save(equipment);
    return EquipmentMapper.toEquipmentResponseDTO(saved);
  }

  public EquipmentResponseDTO getEquipmentById(Long id) {
    Equipment equipment = equipmentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Equipment not found"));
    return EquipmentMapper.toEquipmentResponseDTO(equipment);
  }

  public List<EquipmentResponseDTO> listEquipment() {
    return equipmentRepository.findAll().stream().map(EquipmentMapper::toEquipmentResponseDTO)
        .collect(Collectors.toList());
  }

  public List<EquipmentResponseDTO> listEquipmentByCourt(Long courtId) {
    return equipmentRepository.findAll().stream()
        .filter(e -> e.getCourt() != null && e.getCourt().getCourtId().equals(courtId))
        .map(EquipmentMapper::toEquipmentResponseDTO)
        .collect(Collectors.toList());
  }

  public EquipmentResponseDTO updateEquipment(Long id, EquipmentRequestDTO dto) {
    Equipment equipment = equipmentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Equipment not found"));
    equipment.setName(dto.getName());
    equipment.setRatePerHour(dto.getRatePerHour());
    equipment.setAvailableQuantity(dto.getAvailableQuantity());
    Equipment saved = equipmentRepository.save(equipment);
    return EquipmentMapper.toEquipmentResponseDTO(saved);
  }

  public void deleteEquipment(Long id) {
    equipmentRepository.deleteById(id);
  }
}