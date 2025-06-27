package com.zanar.playera.controller;

import com.zanar.playera.dto.EquipmentRequestDTO;
import com.zanar.playera.dto.EquipmentResponseDTO;
import com.zanar.playera.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@Tag(name = "Equipment Management", description = "APIs for managing sports equipment and rentals")
@Validated
@CrossOrigin(origins = "*")
public class EquipmentController {
    
    @Autowired
    private EquipmentService equipmentService;

    @GetMapping
    @Operation(summary = "Get all equipment", description = "Retrieve all equipment in the system")
    public ResponseEntity<List<EquipmentResponseDTO>> listEquipment() {
        List<EquipmentResponseDTO> equipment = equipmentService.listEquipment();
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by ID", description = "Retrieve a specific equipment by its ID")
    public ResponseEntity<EquipmentResponseDTO> getEquipmentById(@PathVariable Long id) {
        try {
            EquipmentResponseDTO equipment = equipmentService.getEquipmentById(id);
            return ResponseEntity.ok(equipment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/court/{courtId}")
    @Operation(summary = "Get equipment by court", description = "Retrieve all equipment for a specific court")
    public ResponseEntity<List<EquipmentResponseDTO>> listEquipmentByCourt(@PathVariable Long courtId) {
        List<EquipmentResponseDTO> equipment = equipmentService.listEquipmentByCourt(courtId);
        return ResponseEntity.ok(equipment);
    }
    
    @GetMapping("/court/{courtId}/available")
    @Operation(summary = "Get available equipment by court", description = "Retrieve only available equipment for a specific court")
    public ResponseEntity<List<EquipmentResponseDTO>> listAvailableEquipmentByCourt(@PathVariable Long courtId) {
        List<EquipmentResponseDTO> equipment = equipmentService.listAvailableEquipmentByCourt(courtId);
        return ResponseEntity.ok(equipment);
    }

    @PostMapping
    @Operation(summary = "Create equipment", description = "Create a new equipment item")
    public ResponseEntity<EquipmentResponseDTO> createEquipment(@Valid @RequestBody EquipmentRequestDTO dto) {
        try {
            EquipmentResponseDTO equipment = equipmentService.createEquipment(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(equipment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment", description = "Update an existing equipment item")
    public ResponseEntity<EquipmentResponseDTO> updateEquipment(@PathVariable Long id,
            @Valid @RequestBody EquipmentRequestDTO dto) {
        try {
            EquipmentResponseDTO equipment = equipmentService.updateEquipment(id, dto);
            return ResponseEntity.ok(equipment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete equipment", description = "Delete an equipment item")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        try {
            equipmentService.deleteEquipment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Equipment rental specific endpoints
    
    @GetMapping("/{id}/availability")
    @Operation(summary = "Check equipment availability", description = "Check if equipment is available for rental")
    public ResponseEntity<Boolean> checkEquipmentAvailability(
            @PathVariable Long id,
            @RequestParam @Min(1) int quantity) {
        try {
            boolean isAvailable = equipmentService.isEquipmentAvailable(id, quantity);
            return ResponseEntity.ok(isAvailable);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/calculate-cost")
    @Operation(summary = "Calculate rental cost", description = "Calculate the cost for renting equipment")
    public ResponseEntity<Double> calculateRentalCost(
            @PathVariable Long id,
            @RequestParam @Min(1) int quantity,
            @RequestParam @Min(1) int durationHours) {
        try {
            double cost = equipmentService.calculateRentalCost(id, quantity, durationHours);
            return ResponseEntity.ok(cost);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/calculate-deposit")
    @Operation(summary = "Calculate deposit amount", description = "Calculate the deposit amount for equipment")
    public ResponseEntity<Double> calculateDeposit(
            @PathVariable Long id,
            @RequestParam @Min(1) int quantity) {
        try {
            double deposit = equipmentService.calculateDeposit(id, quantity);
            return ResponseEntity.ok(deposit);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/reserve")
    @Operation(summary = "Reserve equipment", description = "Reserve equipment for a booking")
    public ResponseEntity<Void> reserveEquipment(
            @PathVariable Long id,
            @RequestParam @Min(1) int quantity) {
        try {
            equipmentService.reserveEquipment(id, quantity);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/release")
    @Operation(summary = "Release equipment", description = "Release reserved equipment")
    public ResponseEntity<Void> releaseEquipment(
            @PathVariable Long id,
            @RequestParam @Min(1) int quantity) {
        try {
            equipmentService.releaseEquipment(id, quantity);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}