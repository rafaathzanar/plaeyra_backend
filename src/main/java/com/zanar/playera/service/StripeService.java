package com.zanar.playera.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

  @Value("${stripe.secret-key}")
  private String secretKey;

  @Value("${stripe.publishable-key}")
  private String publishableKey;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  @PostConstruct
  public void init() {
    Stripe.apiKey = secretKey;
  }

  /**
   * Create a PaymentIntent for the given amount
   */
  public PaymentIntent createPaymentIntent(Long amount, String currency, String description, String customerEmail)
      throws StripeException {
    // Convert amount to cents (Stripe expects amounts in smallest currency unit)
    long amountInCents = amount * 100;

    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(amountInCents)
        .setCurrency(currency.toLowerCase())
        .setDescription(description)
        .setReceiptEmail(customerEmail)
        .setAutomaticPaymentMethods(
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .build())
        .putMetadata("source", "playera_booking_app")
        .build();

    return PaymentIntent.create(params);
  }

  /**
   * Retrieve a PaymentIntent by ID
   */
  public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
    return PaymentIntent.retrieve(paymentIntentId);
  }

  /**
   * Confirm a PaymentIntent
   */
  public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
    return paymentIntent.confirm();
  }

  /**
   * Cancel a PaymentIntent
   */
  public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
    return paymentIntent.cancel();
  }

  /**
   * Get publishable key for frontend
   */
  public String getPublishableKey() {
    return publishableKey;
  }

  /**
   * Get webhook secret for webhook verification
   */
  public String getWebhookSecret() {
    return webhookSecret;
  }
}
