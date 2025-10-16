package com.zanar.playera.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.zanar.playera.dto.PaymentRequestDTO;
import com.zanar.playera.dto.PaymentResponseDTO;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.Payment;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.mapper.PaymentMapper;
import com.zanar.playera.repo.PaymentRepository;
import com.zanar.playera.repo.BookingRepository;
import com.zanar.playera.repo.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class PaymentService {

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private StripeService stripeService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private LoyaltyProgramService loyaltyProgramService;

  @Value("${stripe.currency:LKR}")
  private String defaultCurrency;

  @Value("${app.platform-fee-percentage:0.10}")
  private double platformFeePercentage;

  @Value("${app.stripe-fee-percentage:0.029}")
  private double stripeFeePercentage;

  /**
   * Create payment without booking (for existing payment intents)
   */
  public PaymentResponseDTO createPaymentFromIntent(PaymentRequestDTO dto, Long customerId) throws StripeException {
    log.info("=== CREATE PAYMENT FROM INTENT DEBUG ===");
    log.info("Creating payment for intent: {}, amount: {}, status: {}, customerId: {}",
        dto.getTransactionId(), dto.getAmount(), dto.getStatus(), customerId);

    // Get customer details
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

    // Create payment entity without booking (will be linked later)
    Payment payment = new Payment();
    payment.setAmount(dto.getAmount());
    payment.setCurrency(defaultCurrency);
    payment.setStatus(
        Payment.PaymentStatus.valueOf(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "SUCCEEDED"));
    payment.setPaymentMethod(
        Payment.PaymentMethod.valueOf(dto.getPaymentMethod() != null ? dto.getPaymentMethod().toUpperCase() : "CARD"));
    payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDateTime.now());
    payment.setDescription(dto.getDescription() != null ? dto.getDescription() : "Payment for booking");
    payment.setTransactionId(dto.getTransactionId());
    payment.setStripePaymentIntentId(dto.getTransactionId()); // Use transaction ID as payment intent ID
    payment.setCustomerEmail(customer.getEmail());
    payment.setCustomerName(customer.getName());
    payment.setCustomerPhone(customer.getPhone());

    // Calculate fees
    payment.calculateFees(platformFeePercentage, stripeFeePercentage);

    // Save payment
    Payment savedPayment = paymentRepository.save(payment);
    log.info("Payment created with ID: {}, status: {}, customer: {} ({})",
        savedPayment.getPaymentId(), savedPayment.getStatus(), customer.getName(), customer.getEmail());
    log.info("=== END CREATE PAYMENT FROM INTENT DEBUG ===");

    return PaymentMapper.toPaymentResponseDTO(savedPayment);
  }

  /**
   * Create a payment for a booking
   */
  public PaymentResponseDTO createPayment(PaymentRequestDTO dto) throws StripeException {
    // Validate booking exists
    Booking booking = bookingRepository.findById(dto.getBookingId())
        .orElseThrow(() -> new RuntimeException("Booking not found"));

    // Validate customer exists
    Customer customer = customerRepository.findById(booking.getCustomer().getUserId())
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    // Create real Stripe payment intent
    com.stripe.model.PaymentIntent stripePaymentIntent = stripeService.createPaymentIntent(
        (long) Math.round(dto.getAmount()),
        defaultCurrency,
        "Payment for booking " + dto.getBookingId(),
        customer.getEmail());
    String paymentIntentId = stripePaymentIntent.getId();

    // Create payment entity
    Payment payment = new Payment();
    payment.setAmount(dto.getAmount());
    payment.setCurrency(defaultCurrency);
    payment
        .setStatus(Payment.PaymentStatus.valueOf(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "PENDING"));
    payment.setPaymentMethod(
        Payment.PaymentMethod.valueOf(dto.getPaymentMethod() != null ? dto.getPaymentMethod().toUpperCase() : "CARD"));
    payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDateTime.now());
    payment.setDescription(
        dto.getDescription() != null ? dto.getDescription() : "Payment for booking " + dto.getBookingId());
    payment.setTransactionId(dto.getTransactionId());
    payment.setStripePaymentIntentId(paymentIntentId);
    payment.setCustomerEmail(customer.getEmail());
    payment.setCustomerName(customer.getName());
    payment.setCustomerPhone(customer.getPhone());
    payment.setBooking(booking);

    // Calculate fees
    payment.calculateFees(platformFeePercentage, stripeFeePercentage);

    // Save payment
    Payment savedPayment = paymentRepository.save(payment);

    // Update booking with payment
    booking.setPayment(savedPayment);
    bookingRepository.save(booking);

    return PaymentMapper.toPaymentResponseDTO(savedPayment);
  }

  /**
   * Process payment confirmation
   */
  public PaymentResponseDTO confirmPayment(String paymentIntentId) throws StripeException {
    log.info("=== PAYMENT CONFIRMATION DEBUG ===");
    log.info("Confirming payment for intent: {}", paymentIntentId);

    // Mock payment confirmation - in real implementation, this would call Stripe
    // API
    // For now, we'll simulate a successful payment

    Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
        .orElseThrow(() -> new RuntimeException("Payment not found"));

    log.info("Found payment: ID={}, Current Status={}, Booking ID={}",
        payment.getPaymentId(), payment.getStatus(),
        payment.getBooking() != null ? payment.getBooking().getBookingId() : "null");

    // Mock successful payment confirmation
    payment.markAsProcessed();
    log.info("Payment marked as processed, new status: {}", payment.getStatus());

    // Set mock charge ID and receipt URL
    String chargeId = paymentIntentId; // Use payment intent ID as charge ID for now
    payment.setStripeChargeId(chargeId);

    // For receipt URL, we'll construct it or get it from metadata
    String receiptUrl = "https://dashboard.stripe.com/payments/" + paymentIntentId;
    payment.setReceiptUrl(receiptUrl);

    Payment savedPayment = paymentRepository.save(payment);
    log.info("Payment saved to database with status: {}", savedPayment.getStatus());

    // Update booking status
    Booking booking = payment.getBooking();
    if (booking != null) {
      log.info("Updating booking status from {} to BOOKED", booking.getBookingStatus());
      booking.setBookingStatus(Booking.BookingStatus.BOOKED);
      bookingRepository.save(booking);
      log.info("Booking status updated successfully");

      // Award loyalty points
      loyaltyProgramService.awardPointsForBooking(
          booking.getCustomer().getUserId(),
          booking.getBookingId());

      // Send confirmation notifications
      sendPaymentConfirmationNotifications(payment);
    } else {
      log.warn("No booking found for payment: {}", payment.getPaymentId());
    }

    PaymentResponseDTO response = PaymentMapper.toPaymentResponseDTO(savedPayment);
    log.info("Payment confirmation completed. Response status: {}", response.getStatus());
    log.info("=== END PAYMENT CONFIRMATION DEBUG ===");

    return response;
  }

  /**
   * Process refund with real Stripe integration
   */
  public PaymentResponseDTO processRefund(Long paymentId, Double refundAmount, String reason) throws StripeException {
    log.info("=== PROCESS REFUND DEBUG ===");
    log.info("Processing refund for payment ID: {}, amount: {}, reason: {}", paymentId, refundAmount, reason);

    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new RuntimeException("Payment not found"));

    log.info("Found payment: ID={}, Amount={}, Status={}, CanRefund={}",
        payment.getPaymentId(), payment.getAmount(), payment.getStatus(), payment.canRefund());

    if (!payment.canRefund()) {
      throw new RuntimeException("Payment cannot be refunded");
    }

    // Convert refund amount to cents for Stripe
    Long refundAmountInCents = Math.round(refundAmount * 100);
    log.info("Refund amount in cents: {}", refundAmountInCents);

    // Create refund in Stripe
    com.stripe.model.Refund stripeRefund = stripeService.createRefund(
        payment.getStripePaymentIntentId(),
        refundAmountInCents,
        reason);

    log.info("Stripe refund created: ID={}, Status={}", stripeRefund.getId(), stripeRefund.getStatus());

    // Update payment status based on refund amount
    if (refundAmount.equals(payment.getAmount())) {
      payment.markAsRefunded(refundAmount, reason);
      log.info("Payment marked as fully refunded");
    } else {
      payment.markAsPartiallyRefunded(refundAmount, reason);
      log.info("Payment marked as partially refunded");
    }

    // Store Stripe refund ID
    payment.setStripeRefundId(stripeRefund.getId());
    payment.setRefundNotes(reason);

    Payment savedPayment = paymentRepository.save(payment);
    log.info("Payment saved with refund amount: {}, refund status: {}",
        savedPayment.getRefundAmount(), savedPayment.getStatus());

    // Update booking status if full refund
    if (payment.isFullRefund()) {
      Booking booking = payment.getBooking();
      if (booking != null) {
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking status updated to CANCELLED");
      }
    }

    // Send refund notification
    sendRefundNotification(savedPayment);

    log.info("=== END PROCESS REFUND DEBUG ===");
    return PaymentMapper.toPaymentResponseDTO(savedPayment);
  }

  /**
   * Calculate refund amount based on cancellation policy
   */
  public Double calculateRefundAmount(Long bookingId, String cancellationReason) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new RuntimeException("Booking not found"));

    Payment payment = booking.getPayment();
    if (payment == null) {
      return 0.0;
    }

    LocalDateTime bookingDateTime = booking.getBookingDate();
    LocalDateTime currentTime = LocalDateTime.now();
    long hoursUntilBooking = java.time.Duration.between(currentTime, bookingDateTime).toHours();

    // Get venue refund policy
    String refundPolicy = null;
    if (booking.getBookingCourts() != null && !booking.getBookingCourts().isEmpty()) {
      refundPolicy = booking.getBookingCourts().get(0).getCourt().getVenue().getRefundPolicy();
    }

    // Default refund policy based on time
    double refundPercentage = 0.0;

    if (hoursUntilBooking >= 24) {
      refundPercentage = 1.0; // Full refund if cancelled 24+ hours before
    } else if (hoursUntilBooking >= 6) {
      refundPercentage = 0.5; // 50% refund if cancelled 6-24 hours before
    } else {
      refundPercentage = 0.0; // No refund if cancelled less than 6 hours before
    }

    // Override with venue-specific policy if available
    if (refundPolicy != null && !refundPolicy.isEmpty()) {
      // Parse venue refund policy (could be JSON or simple text)
      // For now, use default policy
    }

    return payment.getAmount() * refundPercentage;
  }

  /**
   * Send refund notification
   */
  private void sendRefundNotification(Payment payment) {
    try {
      notificationService.sendRefundConfirmation(
          payment.getCustomerEmail(),
          payment.getCustomerName(),
          payment.getRefundAmount(),
          payment.getCurrency(),
          payment.getRefundReason(),
          payment.getBooking() != null ? payment.getBooking().getBookingId().toString() : "N/A");
    } catch (Exception e) {
      System.err.println("Failed to send refund notification: " + e.getMessage());
    }
  }

  /**
   * Get payment by ID
   */
  public PaymentResponseDTO getPaymentById(Long id) {
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Payment not found"));
    return PaymentMapper.toPaymentResponseDTO(payment);
  }

  /**
   * Get payment by Stripe payment intent ID
   */
  public PaymentResponseDTO getPaymentByStripeIntentId(String paymentIntentId) {
    Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
        .orElseThrow(() -> new RuntimeException("Payment not found"));
    return PaymentMapper.toPaymentResponseDTO(payment);
  }

  /**
   * List all payments
   */
  public List<PaymentResponseDTO> listPayments() {
    return paymentRepository.findAll().stream()
        .map(PaymentMapper::toPaymentResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get payments by customer
   */
  public List<PaymentResponseDTO> getPaymentsByCustomer(Long customerId) {
    return paymentRepository.findAll().stream()
        .filter(p -> p.getCustomerEmail() != null &&
            customerRepository.findById(customerId)
                .map(c -> c.getEmail().equals(p.getCustomerEmail()))
                .orElse(false))
        .map(PaymentMapper::toPaymentResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get payments by venue owner
   */
  public List<PaymentResponseDTO> getPaymentsByVenueOwner(Long ownerId) {
    return paymentRepository.findAll().stream()
        .filter(p -> p.getBooking() != null &&
            p.getBooking().getBookingCourts() != null &&
            p.getBooking().getBookingCourts().stream()
                .anyMatch(bc -> bc.getCourt().getVenue().getVenueOwner().getUserId().equals(ownerId)))
        .map(PaymentMapper::toPaymentResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get payment statistics
   */
  public PaymentStatsDTO getPaymentStats() {
    List<Payment> payments = paymentRepository.findAll();

    PaymentStatsDTO stats = new PaymentStatsDTO();
    stats.setTotalPayments(payments.size());
    stats.setTotalAmount(payments.stream()
        .filter(p -> p.isSuccessful())
        .mapToDouble(Payment::getAmount)
        .sum());
    stats.setSuccessfulPayments((int) payments.stream()
        .filter(Payment::isSuccessful)
        .count());
    stats.setFailedPayments((int) payments.stream()
        .filter(Payment::isFailed)
        .count());
    stats.setRefundedPayments((int) payments.stream()
        .filter(Payment::isRefunded)
        .count());

    return stats;
  }

  /**
   * Send payment confirmation notifications
   */
  private void sendPaymentConfirmationNotifications(Payment payment) {
    try {
      notificationService.sendPaymentConfirmation(
          payment.getCustomerEmail(),
          payment.getCustomerName(),
          payment.getAmount(),
          payment.getCurrency(),
          payment.getBooking() != null ? payment.getBooking().getBookingId().toString() : "N/A");
    } catch (Exception e) {
      System.err.println("Failed to send payment confirmation notification: " + e.getMessage());
    }
  }

  /**
   * Send payment failure notifications
   */
  private void sendPaymentFailureNotifications(Payment payment) {
    try {
      notificationService.sendPaymentFailure(
          payment.getCustomerEmail(),
          payment.getCustomerName(),
          payment.getFailureReason(),
          payment.getBooking() != null ? payment.getBooking().getBookingId().toString() : "N/A");
    } catch (Exception e) {
      System.err.println("Failed to send payment failure notification: " + e.getMessage());
    }
  }

  /**
   * Payment statistics DTO
   */
  public static class PaymentStatsDTO {
    private int totalPayments;
    private double totalAmount;
    private int successfulPayments;
    private int failedPayments;
    private int refundedPayments;

    // Getters and setters
    public int getTotalPayments() {
      return totalPayments;
    }

    public void setTotalPayments(int totalPayments) {
      this.totalPayments = totalPayments;
    }

    public double getTotalAmount() {
      return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
      this.totalAmount = totalAmount;
    }

    public int getSuccessfulPayments() {
      return successfulPayments;
    }

    public void setSuccessfulPayments(int successfulPayments) {
      this.successfulPayments = successfulPayments;
    }

    public int getFailedPayments() {
      return failedPayments;
    }

    public void setFailedPayments(int failedPayments) {
      this.failedPayments = failedPayments;
    }

    public int getRefundedPayments() {
      return refundedPayments;
    }

    public void setRefundedPayments(int refundedPayments) {
      this.refundedPayments = refundedPayments;
    }
  }
}
