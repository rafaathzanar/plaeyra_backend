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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
        return equipmentRepository.findAll().stream()
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> listEquipmentByCourt(Long courtId) {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getCourt() != null && e.getCourt().getCourtId().equals(courtId))
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> listAvailableEquipmentByCourt(Long courtId) {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getCourt() != null && e.getCourt().getCourtId().equals(courtId))
                .filter(Equipment::isAvailable)
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> listAvailableEquipment() {
        return equipmentRepository.findAll().stream()
                .filter(Equipment::isAvailable)
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public EquipmentResponseDTO updateEquipment(Long id, EquipmentRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));

        equipment.setName(dto.getName());
        equipment.setDescription(dto.getDescription());
        equipment.setRatePerHour(dto.getRatePerHour());
        equipment.setTotalQuantity(dto.getTotalQuantity());
        equipment.setAvailableQuantity(dto.getAvailableQuantity());
        equipment.setLastMaintenanceDate(dto.getLastMaintenanceDate());

        Equipment saved = equipmentRepository.save(equipment);
        return EquipmentMapper.toEquipmentResponseDTO(saved);
    }

    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
    }

    // Equipment rental specific methods

    public boolean isEquipmentAvailable(Long equipmentId, int requestedQuantity) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        return equipment.canRent(requestedQuantity, 1);
    }

    public double calculateRentalCost(Long equipmentId, int quantity, int durationHours) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        return equipment.calculateRentalCost(quantity, durationHours);
    }

    public void reserveEquipment(Long equipmentId, int quantity) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        equipment.reserve(quantity);
        equipmentRepository.save(equipment);
    }

    public void releaseEquipment(Long equipmentId, int quantity) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        equipment.release(quantity);
        equipmentRepository.save(equipment);
    }

    public void updateEquipmentStatus(Long equipmentId, Equipment.EquipmentStatus status) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        equipment.setStatus(status);
        equipmentRepository.save(equipment);
    }

    public void updateEquipmentMaintenance(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found"));
        equipment.setLastMaintenanceDate(java.time.LocalDateTime.now());
        equipment.setStatus(Equipment.EquipmentStatus.MAINTENANCE);
        equipmentRepository.save(equipment);
    }

    public List<EquipmentResponseDTO> getEquipmentByStatus(Equipment.EquipmentStatus status) {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == status)
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> getEquipmentByVenue(Long venueId) {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getCourt() != null && e.getCourt().getVenue() != null &&
                        e.getCourt().getVenue().getVenueId().equals(venueId))
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentResponseDTO> getEquipmentNeedingMaintenance() {
        return equipmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == Equipment.EquipmentStatus.MAINTENANCE)
                .map(EquipmentMapper::toEquipmentResponseDTO)
                .collect(Collectors.toList());
    }

    public double getTotalEquipmentValue() {
        return equipmentRepository.findAll().stream()
                .mapToDouble(e -> e.getRatePerHour() * e.getTotalQuantity())
                .sum();
    }

    public int getTotalAvailableEquipment() {
        return equipmentRepository.findAll().stream()
                .mapToInt(Equipment::getAvailableQuantity)
                .sum();
    }

    public int getTotalRentedEquipment() {
        return equipmentRepository.findAll().stream()
                .mapToInt(Equipment::getRentedQuantity)
                .sum();
    }
}