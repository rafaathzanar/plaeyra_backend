package com.zanar.playera.controller;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.dto.PaymentIntentRequestDTO;
import com.zanar.playera.dto.PaymentIntentResponseDTO;
import com.zanar.playera.service.PaymentService;
import com.zanar.playera.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private StripeService stripeService;

  @GetMapping
  public ResponseEntity<List<PaymentResponseDTO>> listPayments() {
    return ResponseEntity.ok(paymentService.listPayments());
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable Long id) {
    return ResponseEntity.ok(paymentService.getPaymentById(id));
  }

  @PostMapping
  public ResponseEntity<?> createPayment(@RequestBody PaymentRequestDTO dto) {
    try {
      PaymentResponseDTO payment = paymentService.createPayment(dto);
      return ResponseEntity.ok(payment);
    } catch (StripeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment creation failed: " + e.getMessage());
      errorResponse.put("stripeError", e.getStripeError() != null ? e.getStripeError().getCode() : "unknown");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment creation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * Create a Stripe PaymentIntent for mobile app
   */
  @PostMapping("/create-intent")
  public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequestDTO request) {
    try {
      PaymentIntent paymentIntent = stripeService.createPaymentIntent(
          request.getAmount(),
          request.getCurrency(),
          request.getDescription(),
          request.getCustomerEmail());

      PaymentIntentResponseDTO response = new PaymentIntentResponseDTO(
          paymentIntent.getId(),
          paymentIntent.getClientSecret(),
          paymentIntent.getStatus(),
          request.getAmount(),
          request.getCurrency(),
          request.getDescription(),
          request.getCustomerEmail(),
          stripeService.getPublishableKey());

      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment intent creation failed: " + e.getMessage());
      errorResponse.put("stripeError", e.getStripeError() != null ? e.getStripeError().getCode() : "unknown");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment intent creation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * Confirm a PaymentIntent
   */
  @PostMapping("/confirm-intent/{paymentIntentId}")
  public ResponseEntity<?> confirmPaymentIntent(@PathVariable String paymentIntentId) {
    try {
      PaymentIntent paymentIntent = stripeService.confirmPaymentIntent(paymentIntentId);

      Map<String, Object> response = new HashMap<>();
      response.put("paymentIntentId", paymentIntent.getId());
      response.put("status", paymentIntent.getStatus());
      response.put("amount", paymentIntent.getAmount());
      response.put("currency", paymentIntent.getCurrency());

      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment confirmation failed: " + e.getMessage());
      errorResponse.put("stripeError", e.getStripeError() != null ? e.getStripeError().getCode() : "unknown");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment confirmation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * Get PaymentIntent status
   */
  @GetMapping("/intent/{paymentIntentId}")
  public ResponseEntity<?> getPaymentIntentStatus(@PathVariable String paymentIntentId) {
    try {
      PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);

      Map<String, Object> response = new HashMap<>();
      response.put("paymentIntentId", paymentIntent.getId());
      response.put("status", paymentIntent.getStatus());
      response.put("amount", paymentIntent.getAmount());
      response.put("currency", paymentIntent.getCurrency());
      response.put("clientSecret", paymentIntent.getClientSecret());

      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Failed to retrieve payment intent: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Failed to retrieve payment intent: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * Cancel a PaymentIntent
   */
  @PostMapping("/cancel-intent/{paymentIntentId}")
  public ResponseEntity<?> cancelPaymentIntent(@PathVariable String paymentIntentId) {
    try {
      PaymentIntent paymentIntent = stripeService.cancelPaymentIntent(paymentIntentId);

      Map<String, Object> response = new HashMap<>();
      response.put("paymentIntentId", paymentIntent.getId());
      response.put("status", paymentIntent.getStatus());

      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment cancellation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Payment cancellation failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }
}
