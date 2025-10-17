package com.zanar.playera.service;

import com.zanar.playera.dto.*;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.LoyaltyTransaction;
import com.zanar.playera.repo.CustomerRepository;
import com.zanar.playera.repo.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

  private final CustomerRepository customerRepository;
  private final LoyaltyTransactionRepository loyaltyTransactionRepository;

  @Transactional(readOnly = true)
  public LoyaltyInfoDTO getLoyaltyInfo(Long customerId) {
    log.info("Getting loyalty info for customer: {}", customerId);

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    LoyaltyInfoDTO.LoyaltyInfoDTOBuilder builder = LoyaltyInfoDTO.builder()
        .customerId(customer.getUserId())
        .customerName(customer.getName())
        .customerEmail(customer.getEmail())
        .currentPoints(customer.getLoyaltyPoints())
        .goldCoins(customer.getGoldCoins())
        .currentTier(customer.getLoyaltyTier().name())
        .discountMultiplier(customer.getDiscountMultiplier())
        .goldCoinMultiplier(customer.getLoyaltyTier().getGoldCoinMultiplier())
        .tierUpdatedAt(customer.getLoyaltyTierUpdatedAt())
        .totalBookings(customer.getTotalBookings())
        .totalSpent(customer.getTotalSpent())
        .lastBookingAt(customer.getLastBookingAt());

    // Calculate points to next tier
    Customer.LoyaltyTier currentTier = customer.getLoyaltyTier();
    Customer.LoyaltyTier nextTier = getNextTier(currentTier);

    if (nextTier != null) {
      builder.pointsToNextTier(nextTier.getThreshold() - customer.getLoyaltyPoints())
          .nextTier(nextTier.name());
    } else {
      builder.pointsToNextTier(0)
          .nextTier("MAX TIER");
    }

    // Get recent transactions
    List<LoyaltyTransaction> recentTransactions = loyaltyTransactionRepository
        .findRecentTransactionsByCustomer(customerId);
    builder.recentTransactions(mapTransactionsToDTO(recentTransactions));

    // Get current benefits
    builder.currentBenefits(getTierBenefits(currentTier));

    return builder.build();
  }

  @Transactional
  public LoyaltyEarningDTO earnFromBooking(Long customerId, Double bookingAmount, String bookingId) {
    log.info("Processing loyalty earning for customer: {}, amount: {}, booking: {}",
        customerId, bookingAmount, bookingId);

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    log.info("Found customer: {}, current goldCoins: {}, loyaltyTier: {}",
        customer.getName(), customer.getGoldCoins(), customer.getLoyaltyTier());

    // Calculate earnings
    int pointsEarned = (int) (bookingAmount * 10); // 10 points per LKR
    int goldCoinsEarned = (int) (bookingAmount * customer.getLoyaltyTier().getGoldCoinMultiplier());

    log.info("Calculated earnings - Points: {}, Gold Coins: {}", pointsEarned, goldCoinsEarned);

    // Update customer
    customer.addBooking(bookingAmount);

    log.info("After addBooking - Customer goldCoins: {}, totalBookings: {}",
        customer.getGoldCoins(), customer.getTotalBookings());

    // Create transaction record
    LoyaltyTransaction transaction = LoyaltyTransaction.builder()
        .customer(customer)
        .transactionType(LoyaltyTransaction.TransactionType.BOOKING_EARNED)
        .pointsChange(pointsEarned)
        .goldCoinsChange(goldCoinsEarned)
        .pointsBalance(customer.getLoyaltyPoints())
        .goldCoinsBalance(customer.getGoldCoins())
        .description(String.format("Booking completed - LKR %.2f", bookingAmount))
        .referenceId(bookingId)
        .transactionDate(LocalDateTime.now())
        .build();

    loyaltyTransactionRepository.save(transaction);
    customerRepository.save(customer);

    log.info("Loyalty earning processed - Points: {}, Gold Coins: {}, Transaction saved",
        pointsEarned, goldCoinsEarned);

    return LoyaltyEarningDTO.builder()
        .customerId(customerId)
        .pointsEarned(pointsEarned)
        .goldCoinsEarned(goldCoinsEarned)
        .earningType("BOOKING")
        .description(String.format("Earned from booking LKR %.2f", bookingAmount))
        .referenceId(bookingId)
        .build();
  }

  @Transactional
  public LoyaltyEarningDTO earnFromReview(Long customerId, String reviewId) {
    log.info("Processing loyalty earning from review for customer: {}, review: {}",
        customerId, reviewId);

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    // Review rewards
    int pointsEarned = 50; // Fixed points for reviews
    int goldCoinsEarned = 10; // Fixed gold coins for reviews

    // Update customer
    customer.addLoyaltyPoints(pointsEarned);
    customer.setGoldCoins(customer.getGoldCoins() + goldCoinsEarned);

    // Create transaction record
    LoyaltyTransaction transaction = LoyaltyTransaction.builder()
        .customer(customer)
        .transactionType(LoyaltyTransaction.TransactionType.REVIEW_EARNED)
        .pointsChange(pointsEarned)
        .goldCoinsChange(goldCoinsEarned)
        .pointsBalance(customer.getLoyaltyPoints())
        .goldCoinsBalance(customer.getGoldCoins())
        .description("Review submitted")
        .referenceId(reviewId)
        .transactionDate(LocalDateTime.now())
        .build();

    loyaltyTransactionRepository.save(transaction);
    customerRepository.save(customer);

    return LoyaltyEarningDTO.builder()
        .customerId(customerId)
        .pointsEarned(pointsEarned)
        .goldCoinsEarned(goldCoinsEarned)
        .earningType("REVIEW")
        .description("Earned from review submission")
        .referenceId(reviewId)
        .build();
  }

  @Transactional
  public GoldCoinRedemptionDTO redeemGoldCoins(Long customerId, Integer coinsToRedeem, String bookingReference) {
    log.info("Processing gold coin redemption for customer: {}, coins: {}, booking: {}",
        customerId, coinsToRedeem, bookingReference);

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    if (!customer.canRedeemGoldCoins(coinsToRedeem)) {
      throw new RuntimeException("Insufficient gold coins");
    }

    double discountAmount = customer.calculateGoldCoinDiscount(coinsToRedeem);

    // Update customer
    customer.redeemGoldCoins(coinsToRedeem);

    // Create transaction record
    LoyaltyTransaction transaction = LoyaltyTransaction.builder()
        .customer(customer)
        .transactionType(LoyaltyTransaction.TransactionType.REDEMPTION_USED)
        .pointsChange(0)
        .goldCoinsChange(-coinsToRedeem)
        .pointsBalance(customer.getLoyaltyPoints())
        .goldCoinsBalance(customer.getGoldCoins())
        .description(String.format("Gold coins redeemed - %d coins for LKR %.2f discount",
            coinsToRedeem, discountAmount))
        .referenceId(bookingReference)
        .transactionDate(LocalDateTime.now())
        .build();

    loyaltyTransactionRepository.save(transaction);
    customerRepository.save(customer);

    return GoldCoinRedemptionDTO.builder()
        .customerId(customerId)
        .coinsToRedeem(coinsToRedeem)
        .discountAmount(discountAmount)
        .description(String.format("Redeemed %d gold coins for LKR %.2f discount",
            coinsToRedeem, discountAmount))
        .bookingReference(bookingReference)
        .build();
  }

  @Transactional(readOnly = true)
  public Double calculateAvailableDiscount(Long customerId, Double bookingAmount) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    // Tier-based discount
    double tierDiscount = bookingAmount * (1 - customer.getDiscountMultiplier());

    // Gold coin discount (1 coin = 1 LKR)
    double maxGoldCoinDiscount = Math.min(customer.getGoldCoins(), bookingAmount);

    return tierDiscount + maxGoldCoinDiscount;
  }

  @Transactional(readOnly = true)
  public List<LoyaltyTransactionDTO> getTransactionHistory(Long customerId, int limit) {
    List<LoyaltyTransaction> transactions = loyaltyTransactionRepository
        .findByCustomerUserIdOrderByTransactionDateDesc(customerId);

    if (limit > 0 && transactions.size() > limit) {
      transactions = transactions.subList(0, limit);
    }

    return mapTransactionsToDTO(transactions);
  }

  private Customer.LoyaltyTier getNextTier(Customer.LoyaltyTier currentTier) {
    switch (currentTier) {
      case BRONZE:
        return Customer.LoyaltyTier.SILVER;
      case SILVER:
        return Customer.LoyaltyTier.GOLD;
      case GOLD:
        return Customer.LoyaltyTier.PLATINUM;
      case PLATINUM:
      default:
        return null; // Max tier reached
    }
  }

  private List<TierBenefitDTO> getTierBenefits(Customer.LoyaltyTier tier) {
    List<TierBenefitDTO> benefits = new ArrayList<>();

    switch (tier) {
      case PLATINUM:
        benefits.addAll(Arrays.asList(
            TierBenefitDTO.builder().benefit("15% Discount").description("15% off all bookings").isActive(true).build(),
            TierBenefitDTO.builder().benefit("2x Gold Coins").description("Earn 2x gold coins per booking")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Priority Support").description("24/7 priority customer support")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Free Equipment").description("Free equipment rental up to 2 hours")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Exclusive Access").description("Access to exclusive venues and events")
                .isActive(true).build()));
        break;
      case GOLD:
        benefits.addAll(Arrays.asList(
            TierBenefitDTO.builder().benefit("10% Discount").description("10% off all bookings").isActive(true).build(),
            TierBenefitDTO.builder().benefit("1.5x Gold Coins").description("Earn 1.5x gold coins per booking")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Priority Support").description("Priority customer support").isActive(true)
                .build(),
            TierBenefitDTO.builder().benefit("Free Equipment").description("Free equipment rental up to 1 hour")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Advance Booking").description("Book up to 60 days in advance")
                .isActive(true).build()));
        break;
      case SILVER:
        benefits.addAll(Arrays.asList(
            TierBenefitDTO.builder().benefit("5% Discount").description("5% off all bookings").isActive(true).build(),
            TierBenefitDTO.builder().benefit("1.2x Gold Coins").description("Earn 1.2x gold coins per booking")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Standard Support").description("Standard customer support").isActive(true)
                .build(),
            TierBenefitDTO.builder().benefit("Advance Booking").description("Book up to 45 days in advance")
                .isActive(true).build()));
        break;
      case BRONZE:
      default:
        benefits.addAll(Arrays.asList(
            TierBenefitDTO.builder().benefit("No Discount").description("No automatic discount").isActive(false)
                .build(),
            TierBenefitDTO.builder().benefit("1x Gold Coins").description("Earn 1x gold coins per booking")
                .isActive(true).build(),
            TierBenefitDTO.builder().benefit("Standard Support").description("Standard customer support").isActive(true)
                .build(),
            TierBenefitDTO.builder().benefit("Advance Booking").description("Book up to 30 days in advance")
                .isActive(true).build()));
        break;
    }

    return benefits;
  }

  private List<LoyaltyTransactionDTO> mapTransactionsToDTO(List<LoyaltyTransaction> transactions) {
    return transactions.stream()
        .map(transaction -> LoyaltyTransactionDTO.builder()
            .transactionId(transaction.getTransactionId())
            .transactionType(transaction.getTransactionType().name())
            .displayName(transaction.getTransactionType().getDisplayName())
            .pointsChange(transaction.getPointsChange())
            .goldCoinsChange(transaction.getGoldCoinsChange())
            .pointsBalance(transaction.getPointsBalance())
            .goldCoinsBalance(transaction.getGoldCoinsBalance())
            .description(transaction.getDescription())
            .referenceId(transaction.getReferenceId())
            .transactionDate(transaction.getTransactionDate())
            .build())
        .toList();
  }

  /**
   * Fixes null goldCoins for all customers by setting them to 0
   * This is a one-time migration method to handle existing data
   */
  public void fixNullGoldCoins() {
    log.info("Starting gold coins fix for all customers");

    List<Customer> customersWithNullGoldCoins = customerRepository.findAll()
        .stream()
        .filter(Customer::hasNullGoldCoins)
        .toList();

    log.info("Found {} customers with null gold coins", customersWithNullGoldCoins.size());

    for (Customer customer : customersWithNullGoldCoins) {
      customer.setGoldCoins(0);
      customerRepository.save(customer);
      log.info("Fixed gold coins for customer: {} (ID: {})", customer.getName(), customer.getUserId());
    }

    log.info("Gold coins fix completed for {} customers", customersWithNullGoldCoins.size());
  }
}
