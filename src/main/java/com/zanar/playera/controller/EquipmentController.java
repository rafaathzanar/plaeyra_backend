package com.zanar.playera.controller;

import com.zanar.playera.dto.EquipmentRequestDTO;
import com.zanar.playera.dto.EquipmentResponseDTO;
import com.zanar.playera.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
  @Autowired
  private EquipmentService equipmentService;

  @GetMapping
  public ResponseEntity<List<EquipmentResponseDTO>> listEquipment() {
    return ResponseEntity.ok(equipmentService.listEquipment());
  }

  @GetMapping("/{id}")
  public ResponseEntity<EquipmentResponseDTO> getEquipmentById(@PathVariable Long id) {
    return ResponseEntity.ok(equipmentService.getEquipmentById(id));
  }

  @GetMapping("/court/{courtId}")
  public ResponseEntity<List<EquipmentResponseDTO>> listEquipmentByCourt(@PathVariable Long courtId) {
    return ResponseEntity.ok(equipmentService.listEquipmentByCourt(courtId));
  }

  @PostMapping
  public ResponseEntity<EquipmentResponseDTO> createEquipment(@RequestBody EquipmentRequestDTO dto) {
    return ResponseEntity.ok(equipmentService.createEquipment(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<EquipmentResponseDTO> updateEquipment(@PathVariable Long id,
      @RequestBody EquipmentRequestDTO dto) {
    return ResponseEntity.ok(equipmentService.updateEquipment(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
    equipmentService.deleteEquipment(id);
    return ResponseEntity.noContent().build();
  }
}