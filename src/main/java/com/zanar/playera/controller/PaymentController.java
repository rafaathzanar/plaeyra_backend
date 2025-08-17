package com.zanar.playera.controller;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.service.PaymentService;
import com.zanar.playera.service.StripeService;
import com.stripe.exception.StripeException;
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

  @PostMapping("/mock-intent")
  public ResponseEntity<Map<String, String>> createMockPaymentIntent(@RequestBody Map<String, Object> req) {
    int amount = (int) req.getOrDefault("amount", 0);
    String currency = (String) req.getOrDefault("currency", "usd");
    String description = (String) req.getOrDefault("description", "");
    String clientSecret = stripeService.createMockPaymentIntent(amount, currency, description);
    return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
  }
}
