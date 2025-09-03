package com.zanar.playera.controller;

import com.zanar.playera.dto.NotificationDTO;
import com.zanar.playera.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

  @Autowired
  private NotificationService notificationService;

  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  @Operation(summary = "Get user notifications", description = "Retrieves all notifications for the authenticated user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<NotificationDTO>> getUserNotifications(@RequestParam Long userId) {
    try {
      List<NotificationDTO> notifications = notificationService.getUserNotifications(userId);
      return ResponseEntity.ok(notifications);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/unread-count")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  @Operation(summary = "Get unread notification count", description = "Gets the count of unread notifications for the user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Long> getUnreadCount(@RequestParam Long userId) {
    try {
      long count = notificationService.getUnreadNotificationCount(userId);
      return ResponseEntity.ok(count);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{notificationId}/read")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notification marked as read"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Notification not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, @RequestParam Long userId) {
    try {
      notificationService.markAsRead(notificationId, userId);
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{notificationId}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'VENUE_OWNER', 'ADMIN')")
  @Operation(summary = "Delete notification", description = "Deletes a specific notification")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Notification deleted"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
      @ApiResponse(responseCode = "404", description = "Notification not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId, @RequestParam Long userId) {
    try {
      notificationService.deleteNotification(notificationId, userId);
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
