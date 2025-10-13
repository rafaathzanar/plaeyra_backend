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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
    // Mock payment confirmation - in real implementation, this would call Stripe
    // API
    // For now, we'll simulate a successful payment

    Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
        .orElseThrow(() -> new RuntimeException("Payment not found"));

    // Mock successful payment confirmation
    payment.markAsProcessed();
    // Set mock charge ID and receipt URL
    String chargeId = paymentIntentId; // Use payment intent ID as charge ID for now
    payment.setStripeChargeId(chargeId);

    // For receipt URL, we'll construct it or get it from metadata
    String receiptUrl = "https://dashboard.stripe.com/payments/" + paymentIntentId;
    payment.setReceiptUrl(receiptUrl);

    Payment savedPayment = paymentRepository.save(payment);

    // Update booking status
    Booking booking = payment.getBooking();
    if (booking != null) {
      booking.setBookingStatus(Booking.BookingStatus.BOOKED);
      bookingRepository.save(booking);

      // Award loyalty points
      loyaltyProgramService.awardPointsForBooking(
          booking.getCustomer().getUserId(),
          booking.getBookingId());

      // Send confirmation notifications
      sendPaymentConfirmationNotifications(payment);
    }

    return PaymentMapper.toPaymentResponseDTO(savedPayment);
  }

  /**
   * Process refund
   */
  public PaymentResponseDTO processRefund(Long paymentId, Double refundAmount, String reason) throws StripeException {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new RuntimeException("Payment not found"));

    if (!payment.canRefund()) {
      throw new RuntimeException("Payment cannot be refunded");
    }

    // Mock refund processing (in real implementation, this would call Stripe API)
    // For now, we'll simulate a successful refund

    // Update payment status
    if (refundAmount.equals(payment.getAmount())) {
      payment.markAsRefunded(refundAmount, reason);
    } else {
      payment.markAsPartiallyRefunded(refundAmount, reason);
    }

    payment.setStripeRefundId("mock_refund_" + System.currentTimeMillis());
    payment.setRefundNotes(reason);

    Payment savedPayment = paymentRepository.save(payment);

    // Update booking status if full refund
    if (payment.isFullRefund()) {
      Booking booking = payment.getBooking();
      if (booking != null) {
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
      }
    }

    return PaymentMapper.toPaymentResponseDTO(savedPayment);
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
