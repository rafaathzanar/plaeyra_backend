package com.zanar.playera.controller;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.service.PaymentService;
import com.zanar.playera.service.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<PaymentResponseDTO> createPayment(@RequestBody PaymentRequestDTO dto) {
    return ResponseEntity.ok(paymentService.createPayment(dto));
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
