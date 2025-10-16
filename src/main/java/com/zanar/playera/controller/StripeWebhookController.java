package com.zanar.playera.controller;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.zanar.playera.service.PaymentService;
import com.zanar.playera.service.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments")
public class StripeWebhookController {

  @Autowired
  private StripeService stripeService;

  @Autowired
  private PaymentService paymentService;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  @PostMapping("/webhook")
  public ResponseEntity<String> handleWebhook(
      HttpServletRequest request,
      @RequestBody String payload) {

    String sigHeader = request.getHeader("Stripe-Signature");

    if (sigHeader == null) {
      return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
    }

    try {
      // Verify the webhook signature
      Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

      // Handle the event
      switch (event.getType()) {
        case "payment_intent.succeeded":
          handlePaymentIntentSucceeded(event);
          break;
        case "payment_intent.payment_failed":
          handlePaymentIntentFailed(event);
          break;
        case "payment_intent.canceled":
          handlePaymentIntentCanceled(event);
          break;
        case "charge.dispute.created":
          handleChargeDisputeCreated(event);
          break;
        case "refund.created":
          handleRefundCreated(event);
          break;
        case "refund.updated":
          handleRefundUpdated(event);
          break;
        default:
          System.out.println("Unhandled event type: " + event.getType());
      }

      return ResponseEntity.ok("Webhook processed successfully");

    } catch (SignatureVerificationException e) {
      System.err.println("Invalid signature: " + e.getMessage());
      return ResponseEntity.badRequest().body("Invalid signature");
    } catch (Exception e) {
      System.err.println("Webhook error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Webhook processing failed");
    }
  }

  private void handlePaymentIntentSucceeded(Event event) {
    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

    if (paymentIntent != null) {
      System.out.println("Payment succeeded: " + paymentIntent.getId());
      // Here you can update your database, send notifications, etc.
      // The payment was successful, so you can mark the booking as confirmed
    }
  }

  private void handlePaymentIntentFailed(Event event) {
    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

    if (paymentIntent != null) {
      System.out.println("Payment failed: " + paymentIntent.getId());
      // Handle failed payment - maybe send notification to user
    }
  }

  private void handlePaymentIntentCanceled(Event event) {
    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

    if (paymentIntent != null) {
      System.out.println("Payment canceled: " + paymentIntent.getId());
      // Handle canceled payment
    }
  }

  private void handleChargeDisputeCreated(Event event) {
    com.stripe.model.Dispute dispute = (com.stripe.model.Dispute) event.getDataObjectDeserializer().getObject()
        .orElse(null);

    if (dispute != null) {
      System.out.println("Charge dispute created: " + dispute.getId());
      // Handle dispute - notify venue owner, investigate, etc.
    }
  }

  private void handleRefundCreated(Event event) {
    com.stripe.model.Refund refund = (com.stripe.model.Refund) event.getDataObjectDeserializer().getObject()
        .orElse(null);

    if (refund != null) {
      System.out.println("Refund created: " + refund.getId());
      // Refund was successfully created in Stripe
      // Update our database if needed
    }
  }

  private void handleRefundUpdated(Event event) {
    com.stripe.model.Refund refund = (com.stripe.model.Refund) event.getDataObjectDeserializer().getObject()
        .orElse(null);

    if (refund != null) {
      System.out.println("Refund updated: " + refund.getId());
      // Refund status was updated (e.g., succeeded, failed)
      // Update our database if needed
    }
  }
}
