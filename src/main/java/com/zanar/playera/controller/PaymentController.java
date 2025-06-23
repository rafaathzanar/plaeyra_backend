package com.zanar.playera.controller;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  @Autowired
  private PaymentService paymentService;

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
}
