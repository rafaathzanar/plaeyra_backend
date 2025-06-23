package com.zanar.playera.service;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.entity.Payment;
import com.zanar.playera.mapper.PaymentMapper;
import com.zanar.playera.repo.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {
  @Autowired
  private PaymentRepository paymentRepository;

  public PaymentResponseDTO createPayment(PaymentRequestDTO dto) {
    Payment payment = PaymentMapper.toPaymentEntity(dto);
    Payment saved = paymentRepository.save(payment);
    return PaymentMapper.toPaymentResponseDTO(saved);
  }

  public PaymentResponseDTO getPaymentById(Long id) {
    Payment payment = paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("Payment not found"));
    return PaymentMapper.toPaymentResponseDTO(payment);
  }

  public List<PaymentResponseDTO> listPayments() {
    return paymentRepository.findAll().stream().map(PaymentMapper::toPaymentResponseDTO).collect(Collectors.toList());
  }
}
