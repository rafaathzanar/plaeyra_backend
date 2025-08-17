package com.zanar.playera.mapper;

import org.springframework.stereotype.Component;

import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.entity.Payment;
import java.time.LocalDateTime;

@Component
public class PaymentMapper {
  public static Payment toPaymentEntity(PaymentRequestDTO dto) {
    Payment payment = new Payment();
    payment.setAmount(dto.getAmount());
    payment.setStatus(Payment.PaymentStatus.PENDING);
    payment.setPaymentDate(LocalDateTime.now());
    return payment;
  }

  public static PaymentResponseDTO toPaymentResponseDTO(Payment payment) {
    PaymentResponseDTO dto = new PaymentResponseDTO();
    dto.setPaymentId(payment.getPaymentId());
    dto.setAmount(payment.getAmount());
    dto.setStatus(payment.getStatus().name());
    dto.setPaymentDate(payment.getPaymentDate());
    // bookingId to be set by service/controller if needed
    return dto;
  }
}