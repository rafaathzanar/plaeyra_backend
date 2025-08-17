package com.zanar.playera.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

  @Value("${stripe.secret-key}")
  private String stripeSecretKey;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  public StripeService(@Value("${stripe.secret-key}") String stripeSecretKey) {
    Stripe.apiKey = stripeSecretKey;
  }

  /**
   * Create a payment intent for a booking
   */
  public PaymentIntent createPaymentIntent(Long amount, String currency, String description, String customerEmail)
      throws StripeException {
    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(amount)
        .setCurrency(currency)
        .setDescription(description)
        .setReceiptEmail(customerEmail)
        .setAutomaticPaymentMethods(
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .build())
        .build();

    return PaymentIntent.create(params);
  }

  /**
   * Create a customer in Stripe
   */
  public Customer createCustomer(String email, String name, String phone) throws StripeException {
    CustomerCreateParams params = CustomerCreateParams.builder()
        .setEmail(email)
        .setName(name)
        .setPhone(phone)
        .build();

    return Customer.create(params);
  }

  /**
   * Attach a payment method to a customer
   */
  public void attachPaymentMethodToCustomer(String customerId, String paymentMethodId) throws StripeException {
    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
    PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
        .setCustomer(customerId)
        .build();

    paymentMethod.attach(attachParams);
  }

  /**
   * Confirm a payment intent
   */
  public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
    return PaymentIntent.retrieve(paymentIntentId);
  }

  /**
   * Cancel a payment intent
   */
  public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
    return paymentIntent.cancel();
  }

  /**
   * Process a refund
   */
  public Refund processRefund(String paymentIntentId, Long amount, String reason) throws StripeException {
    RefundCreateParams params = RefundCreateParams.builder()
        .setPaymentIntent(paymentIntentId)
        .setAmount(amount)
        .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
        .setMetadata(Map.of("reason", reason))
        .build();

    return Refund.create(params);
  }

  /**
   * Process a partial refund
   */
  public Refund processPartialRefund(String paymentIntentId, Long amount, String reason) throws StripeException {
    RefundCreateParams params = RefundCreateParams.builder()
        .setPaymentIntent(paymentIntentId)
        .setAmount(amount)
        .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
        .setMetadata(Map.of("reason", reason))
        .build();

    return Refund.create(params);
  }

  /**
   * Get payment intent details
   */
  public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
    return PaymentIntent.retrieve(paymentIntentId);
  }

  /**
   * Get customer details
   */
  public Customer getCustomer(String customerId) throws StripeException {
    return Customer.retrieve(customerId);
  }

  /**
   * Update customer information
   */
  public Customer updateCustomer(String customerId, String name, String phone) throws StripeException {
    Customer customer = Customer.retrieve(customerId);

    Map<String, Object> params = new HashMap<>();
    params.put("name", name);
    params.put("phone", phone);

    return customer.update(params);
  }

  /**
   * Delete a customer
   */
  public void deleteCustomer(String customerId) throws StripeException {
    Customer customer = Customer.retrieve(customerId);
    customer.delete();
  }

  /**
   * Get payment method details
   */
  public PaymentMethod getPaymentMethod(String paymentMethodId) throws StripeException {
    return PaymentMethod.retrieve(paymentMethodId);
  }

  /**
   * Detach a payment method
   */
  public void detachPaymentMethod(String paymentMethodId) throws StripeException {
    PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
    paymentMethod.detach();
  }

  /**
   * List customer payment methods
   */
  public com.stripe.model.PaymentMethodCollection listCustomerPaymentMethods(String customerId) throws StripeException {
    Map<String, Object> params = new HashMap<>();
    params.put("customer", customerId);
    params.put("type", "card");

    return PaymentMethod.list(params);
  }

  /**
   * Create a setup intent for saving payment methods
   */
  public com.stripe.model.SetupIntent createSetupIntent(String customerId) throws StripeException {
    com.stripe.param.SetupIntentCreateParams params = com.stripe.param.SetupIntentCreateParams.builder()
        .setCustomer(customerId)
        .build();

    return com.stripe.model.SetupIntent.create(params);
  }

  /**
   * Validate webhook signature
   */
  public boolean validateWebhookSignature(String payload, String signature) {
    try {
      com.stripe.net.Webhook.Signature.verifyHeader(
          payload, signature, webhookSecret, 300);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Handle successful payment webhook
   */
  public void handleSuccessfulPayment(String paymentIntentId) {
    // This would integrate with your payment service
    // to update booking status, send notifications, etc.
    System.out.println("Payment successful: " + paymentIntentId);
  }

  /**
   * Handle failed payment webhook
   */
  public void handleFailedPayment(String paymentIntentId, String failureReason) {
    // This would integrate with your payment service
    // to handle failed payments, send notifications, etc.
    System.out.println("Payment failed: " + paymentIntentId + " - " + failureReason);
  }

  /**
   * Handle refund webhook
   */
  public void handleRefund(String refundId, String paymentIntentId, Long amount) {
    // This would integrate with your payment service
    // to handle refunds, update booking status, etc.
    System.out.println("Refund processed: " + refundId + " for payment: " + paymentIntentId + " amount: " + amount);
  }
}
