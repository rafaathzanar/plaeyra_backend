package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CUSTOMER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Customer extends User {

    private Integer loyaltyPoints = 0;

    private Integer goldCoins = 0;

    @Enumerated(EnumType.STRING)
    private LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;

    private LocalDateTime loyaltyTierUpdatedAt;

    private String preferredSport;

    private String preferredLocation;

    private Double totalSpent = 0.0;

    private Integer totalBookings = 0;

    private LocalDateTime lastBookingAt;

    private Boolean marketingConsent = false;

    private String emergencyContact;

    private String emergencyPhone;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    public enum LoyaltyTier {
        BRONZE(0, 1.0, 0.02), // 0% discount, 2% gold coins
        SILVER(1000, 0.95, 0.03), // 5% discount, 3% gold coins
        GOLD(5000, 0.90, 0.04), // 10% discount, 4% gold coins
        PLATINUM(15000, 0.85, 0.05); // 15% discount, 5% gold coins

        private final int threshold;
        private final double discountMultiplier;
        private final double goldCoinMultiplier;

        LoyaltyTier(int threshold, double discountMultiplier, double goldCoinMultiplier) {
            this.threshold = threshold;
            this.discountMultiplier = discountMultiplier;
            this.goldCoinMultiplier = goldCoinMultiplier;
        }

        public int getThreshold() {
            return threshold;
        }

        public double getDiscountMultiplier() {
            return discountMultiplier;
        }

        public double getGoldCoinMultiplier() {
            return goldCoinMultiplier;
        }
    }

    // Helper methods for loyalty program
    public void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
        updateLoyaltyTier();
    }

    public void updateLoyaltyTier() {
        LoyaltyTier newTier = LoyaltyTier.BRONZE;

        if (loyaltyPoints >= LoyaltyTier.PLATINUM.getThreshold()) {
            newTier = LoyaltyTier.PLATINUM;
        } else if (loyaltyPoints >= LoyaltyTier.GOLD.getThreshold()) {
            newTier = LoyaltyTier.GOLD;
        } else if (loyaltyPoints >= LoyaltyTier.SILVER.getThreshold()) {
            newTier = LoyaltyTier.SILVER;
        }

        if (newTier != this.loyaltyTier) {
            this.loyaltyTier = newTier;
            this.loyaltyTierUpdatedAt = LocalDateTime.now();
        }
    }

    public double getDiscountMultiplier() {
        return loyaltyTier.getDiscountMultiplier();
    }

    public void addBooking(double amount) {
        this.totalBookings++;
        this.totalSpent += amount;
        this.lastBookingAt = LocalDateTime.now();

        // Add loyalty points based on booking amount
        int pointsEarned = (int) (amount * 0.5); // 0.5 points per LKR (realistic rate)
        addLoyaltyPoints(pointsEarned);

        // Add gold coins based on booking amount and tier multiplier
        int goldCoinsEarned = (int) (amount * loyaltyTier.getGoldCoinMultiplier());
        int currentGoldCoins = (this.goldCoins != null) ? this.goldCoins : 0;
        this.goldCoins = currentGoldCoins + goldCoinsEarned;

        log.info("Customer {} earned {} gold coins (was: {}, now: {})",
                this.getUserId(), goldCoinsEarned, currentGoldCoins, this.goldCoins);
    }

    public boolean canRedeemPoints(int points) {
        return loyaltyPoints >= points;
    }

    public void redeemPoints(int points) {
        if (canRedeemPoints(points)) {
            this.loyaltyPoints -= points;
        } else {
            throw new RuntimeException("Insufficient loyalty points");
        }
    }

    // Gold coin methods
    public boolean canRedeemGoldCoins(int coins) {
        return getGoldCoins() >= coins;
    }

    public void redeemGoldCoins(int coins) {
        if (canRedeemGoldCoins(coins)) {
            this.goldCoins = getGoldCoins() - coins;
        } else {
            throw new RuntimeException("Insufficient gold coins");
        }
    }

    public double calculateGoldCoinDiscount(int coinsToRedeem) {
        // 1 gold coin = 1 LKR discount
        return Math.min(coinsToRedeem, getGoldCoins());
    }

    public int getGoldCoins() {
        return goldCoins != null ? goldCoins : 0;
    }

    public void setGoldCoins(int goldCoins) {
        this.goldCoins = goldCoins;
    }

    public boolean hasNullGoldCoins() {
        return this.goldCoins == null;
    }
}
