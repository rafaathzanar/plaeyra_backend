package com.zanar.playera.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zanar.playera.dto.BookingResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * Send booking update to venue owner
   */
  public void notifyVenueOwner(Long venueId, String message, Object data) {
    try {
      String destination = "/topic/venue/" + venueId + "/bookings";
      messagingTemplate.convertAndSend(destination, data);
    } catch (Exception e) {
      System.err.println("Failed to send WebSocket notification to venue owner: " + e.getMessage());
    }
  }

  /**
   * Send booking update to specific customer
   */
  public void notifyCustomer(Long customerId, String message, Object data) {
    try {
      String destination = "/user/" + customerId + "/queue/bookings";
      messagingTemplate.convertAndSendToUser(customerId.toString(), "/queue/bookings", data);
    } catch (Exception e) {
      System.err.println("Failed to send WebSocket notification to customer: " + e.getMessage());
    }
  }

  /**
   * Send booking confirmation to venue owner
   */
  public void notifyNewBooking(Long venueId, BookingResponseDTO booking) {
    try {
      String destination = "/topic/venue/" + venueId + "/bookings/new";
      messagingTemplate.convertAndSend(destination, booking);
    } catch (Exception e) {
      System.err.println("Failed to send new booking notification: " + e.getMessage());
    }
  }

  /**
   * Send booking status update to venue owner
   */
  public void notifyBookingStatusUpdate(Long venueId, Long bookingId, String newStatus) {
    try {
      String destination = "/topic/venue/" + venueId + "/bookings/" + bookingId + "/status";
      messagingTemplate.convertAndSend(destination, newStatus);
    } catch (Exception e) {
      System.err.println("Failed to send booking status update: " + e.getMessage());
    }
  }

  /**
   * Send booking cancellation to venue owner
   */
  public void notifyBookingCancellation(Long venueId, Long bookingId) {
    try {
      String destination = "/topic/venue/" + venueId + "/bookings/" + bookingId + "/cancelled";
      messagingTemplate.convertAndSend(destination, "CANCELLED");
    } catch (Exception e) {
      System.err.println("Failed to send booking cancellation notification: " + e.getMessage());
    }
  }
}
