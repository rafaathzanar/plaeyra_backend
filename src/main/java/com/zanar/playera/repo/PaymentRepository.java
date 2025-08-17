package com.zanar.playera.repo;

import com.zanar.playera.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}