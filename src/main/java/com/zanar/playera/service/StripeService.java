package com.zanar.playera.service;

import org.springframework.stereotype.Service;

@Service
public class StripeService {
  public String createMockPaymentIntent(int amount, String currency, String description) {
    // Simulate a payment intent creation and return a mock client secret
    return "mock_client_secret_" + System.currentTimeMillis();
  }
}
