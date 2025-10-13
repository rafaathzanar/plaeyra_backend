package com.zanar.playera.controller;

import com.zanar.playera.service.BookingMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

  @Autowired
  private BookingMonitoringService monitoringService;

  /**
   * Get booking system metrics
   */
  @GetMapping("/metrics")
  public ResponseEntity<Map<String, Object>> getMetrics() {
    Map<String, Object> metrics = monitoringService.getMetrics();
    return ResponseEntity.ok(metrics);
  }

  /**
   * Get system health status
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getHealthStatus() {
    Map<String, Object> health = Map.of(
        "status", "UP",
        "timestamp", System.currentTimeMillis(),
        "metrics", monitoringService.getMetrics());
    return ResponseEntity.ok(health);
  }

  /**
   * Trigger manual conflict detection
   */
  @PostMapping("/detect-conflicts")
  public ResponseEntity<Map<String, Object>> detectConflicts() {
    try {
      monitoringService.detectDoubleBookings();
      return ResponseEntity.ok(Map.of(
          "success", true,
          "message", "Conflict detection completed"));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()));
    }
  }
}
