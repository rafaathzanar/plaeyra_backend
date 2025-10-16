package com.zanar.playera.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
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

    System.out.println("StripeService: Creating payment intent");
    System.out.println("Original amount: " + amount + " " + currency);
    System.out.println("Amount in cents: " + amountInCents);

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
   * Create a refund for a payment
   */
  public com.stripe.model.Refund createRefund(String paymentIntentId, Long refundAmount, String reason)
      throws StripeException {
    log.info("=== STRIPE REFUND DEBUG ===");
    log.info("Creating refund for payment intent: {}, amount: {} cents, reason: {}",
        paymentIntentId, refundAmount, reason);

    // First, get the charge ID from the payment intent
    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
    log.info("Retrieved payment intent: {}, status: {}", paymentIntent.getId(), paymentIntent.getStatus());

    if (paymentIntent.getLatestCharge() == null) {
      log.error("No charge found for payment intent: {}", paymentIntentId);
      throw new RuntimeException("No charge found for payment intent: " + paymentIntentId);
    }

    String chargeId = paymentIntent.getLatestCharge();
    log.info("Using charge ID: {}", chargeId);

    // Create refund parameters
    Map<String, Object> refundParams = new HashMap<>();
    refundParams.put("charge", chargeId);
    refundParams.put("amount", refundAmount); // Amount in cents
    refundParams.put("reason", "requested_by_customer"); // Stripe only accepts: duplicate, fraudulent, or
                                                         // requested_by_customer
    refundParams.put("metadata", Map.of(
        "source", "playera_booking_app",
        "refund_reason", reason != null ? reason : "booking_cancellation",
        "booking_cancellation_reason", reason != null ? reason : "booking_cancellation"));

    log.info("Refund parameters: {}", refundParams);

    com.stripe.model.Refund refund = com.stripe.model.Refund.create(refundParams);
    log.info("Stripe refund created: ID={}, Status={}, Amount={}",
        refund.getId(), refund.getStatus(), refund.getAmount());
    log.info("=== END STRIPE REFUND DEBUG ===");

    return refund;
  }

  /**
   * Retrieve a refund by ID
   */
  public com.stripe.model.Refund retrieveRefund(String refundId) throws StripeException {
    return com.stripe.model.Refund.retrieve(refundId);
  }

  /**
   * List refunds for a payment intent
   */
  public com.stripe.model.RefundCollection listRefunds(String paymentIntentId) throws StripeException {
    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

    if (paymentIntent.getLatestCharge() == null) {
      throw new RuntimeException("No charge found for payment intent: " + paymentIntentId);
    }

    String chargeId = paymentIntent.getLatestCharge();

    Map<String, Object> params = new HashMap<>();
    params.put("charge", chargeId);

    return com.stripe.model.Refund.list(params);
  }

  /**
   * Get webhook secret for webhook verification
   */
  public String getWebhookSecret() {
    return webhookSecret;
  }
}
