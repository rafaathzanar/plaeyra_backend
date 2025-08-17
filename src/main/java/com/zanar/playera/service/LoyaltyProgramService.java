package com.zanar.playera.service;

import com.zanar.playera.dto.LoyaltyProgramDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.repo.CustomerRepository;
import com.zanar.playera.repo.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LoyaltyProgramService {

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Value("${loyalty.points-per-booking:100}")
  private int pointsPerBooking;

  @Value("${loyalty.points-per-currency:10}")
  private int pointsPerCurrency;

  @Value("${loyalty.bronze-threshold:0}")
  private int bronzeThreshold;

  @Value("${loyalty.silver-threshold:1000}")
  private int silverThreshold;

  @Value("${loyalty.gold-threshold:5000}")
  private int goldThreshold;

  @Value("${loyalty.platinum-threshold:10000}")
  private int platinumThreshold;

  /**
   * Award points for a completed booking
   */
  public void awardPointsForBooking(Long customerId, Long bookingId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new RuntimeException("Booking not found"));

    // Calculate points based on booking value
    int pointsEarned = (int) (booking.getTotalCost() * pointsPerCurrency);
    pointsEarned += pointsPerBooking; // Bonus points for completing a booking

    customer.addLoyaltyPoints(pointsEarned);
    customer.addBooking(booking.getTotalCost());

    customerRepository.save(customer);
  }

  /**
   * Get customer loyalty information
   */
  public LoyaltyProgramDTO getCustomerLoyaltyInfo(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    LoyaltyProgramDTO dto = new LoyaltyProgramDTO();
    dto.setCustomerId(customerId);
    dto.setCurrentPoints(customer.getLoyaltyPoints());
    dto.setCurrentTier(customer.getLoyaltyTier().name());
    dto.setDiscountMultiplier(customer.getDiscountMultiplier());
    dto.setTierUpdatedAt(customer.getLoyaltyTierUpdatedAt());

    // Calculate points to next tier
    dto.setPointsToNextTier(calculatePointsToNextTier(customer.getLoyaltyPoints()));

    // Get recent transactions
    dto.setRecentTransactions(getRecentTransactions(customerId));

    // Get available rewards
    dto.setAvailableRewards(getAvailableRewards(customerId));

    // Get redeemed rewards
    dto.setRedeemedRewards(getRedeemedRewards(customerId));

    return dto;
  }

  /**
   * Redeem loyalty points
   */
  public boolean redeemPoints(Long customerId, int points, String description) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    if (customer.canRedeemPoints(points)) {
      customer.redeemPoints(points);
      customerRepository.save(customer);

      // Record the transaction
      recordLoyaltyTransaction(customerId, "REDEEM", -points, description);
      return true;
    }

    return false;
  }

  /**
   * Calculate discount based on loyalty tier
   */
  public double calculateLoyaltyDiscount(Long customerId, double originalPrice) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    double discountMultiplier = customer.getDiscountMultiplier();
    return originalPrice * (1 - discountMultiplier);
  }

  /**
   * Check if customer can access premium features
   */
  public boolean canAccessPremiumFeatures(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    return customer.getLoyaltyTier() == Customer.LoyaltyTier.GOLD ||
        customer.getLoyaltyTier() == Customer.LoyaltyTier.PLATINUM;
  }

  /**
   * Get loyalty tier benefits
   */
  public List<String> getTierBenefits(String tierName) {
    List<String> benefits = new ArrayList<>();

    switch (tierName.toUpperCase()) {
      case "PLATINUM":
        benefits.add("20% discount on all bookings");
        benefits.add("Priority customer support");
        benefits.add("Free equipment rental (up to 2 hours)");
        benefits.add("Exclusive venue access");
        benefits.add("Monthly bonus points");
        break;
      case "GOLD":
        benefits.add("15% discount on all bookings");
        benefits.add("Priority customer support");
        benefits.add("Free equipment rental (up to 1 hour)");
        benefits.add("Advance booking (up to 60 days)");
        break;
      case "SILVER":
        benefits.add("10% discount on all bookings");
        benefits.add("Standard customer support");
        benefits.add("Advance booking (up to 45 days)");
        break;
      case "BRONZE":
        benefits.add("5% discount on all bookings");
        benefits.add("Standard customer support");
        benefits.add("Advance booking (up to 30 days)");
        break;
    }

    return benefits;
  }

  /**
   * Calculate points to next tier
   */
  private int calculatePointsToNextTier(int currentPoints) {
    if (currentPoints < silverThreshold) {
      return silverThreshold - currentPoints;
    } else if (currentPoints < goldThreshold) {
      return goldThreshold - currentPoints;
    } else if (currentPoints < platinumThreshold) {
      return platinumThreshold - currentPoints;
    }
    return 0; // Already at highest tier
  }

  /**
   * Get recent loyalty transactions
   */
  private List<LoyaltyProgramDTO.LoyaltyTransactionDTO> getRecentTransactions(Long customerId) {
    // This would integrate with a transaction repository
    // For now, returning empty list
    return new ArrayList<>();
  }

  /**
   * Get available rewards
   */
  private List<LoyaltyProgramDTO.RewardDTO> getAvailableRewards(Long customerId) {
    // This would integrate with a rewards repository
    // For now, returning empty list
    return new ArrayList<>();
  }

  /**
   * Get redeemed rewards
   */
  private List<LoyaltyProgramDTO.RewardDTO> getRedeemedRewards(Long customerId) {
    // This would integrate with a rewards repository
    // For now, returning empty list
    return new ArrayList<>();
  }

  /**
   * Record loyalty transaction
   */
  private void recordLoyaltyTransaction(Long customerId, String type, int points, String description) {
    // This would integrate with a transaction repository
    // For now, just logging
    System.out
        .println("Loyalty transaction recorded: " + customerId + " - " + type + " - " + points + " - " + description);
  }

  /**
   * Get top customers by loyalty points
   */
  public List<Customer> getTopCustomersByLoyaltyPoints(int limit) {
    return customerRepository.findAll().stream()
        .sorted((c1, c2) -> Integer.compare(c2.getLoyaltyPoints(), c1.getLoyaltyPoints()))
        .limit(limit)
        .toList();
  }

  /**
   * Check if customer qualifies for tier upgrade
   */
  public boolean checkTierUpgrade(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));

    Customer.LoyaltyTier currentTier = customer.getLoyaltyTier();
    Customer.LoyaltyTier newTier = currentTier;

    if (customer.getLoyaltyPoints() >= platinumThreshold) {
      newTier = Customer.LoyaltyTier.PLATINUM;
    } else if (customer.getLoyaltyPoints() >= goldThreshold) {
      newTier = Customer.LoyaltyTier.GOLD;
    } else if (customer.getLoyaltyPoints() >= silverThreshold) {
      newTier = Customer.LoyaltyTier.SILVER;
    }

    return newTier != currentTier;
  }
}
