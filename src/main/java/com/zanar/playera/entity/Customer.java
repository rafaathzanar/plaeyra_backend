package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CUSTOMER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends User {

    private int loyaltyPoints = 0;

    @Enumerated(EnumType.STRING)
    private LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;

    private LocalDateTime loyaltyTierUpdatedAt;

    private String preferredSport;

    private String preferredLocation;

    private double totalSpent = 0.0;

    private int totalBookings = 0;

    private LocalDateTime lastBookingAt;

    private boolean marketingConsent = false;

    private String emergencyContact;

    private String emergencyPhone;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    public enum LoyaltyTier {
        BRONZE(0, 1.0),
        SILVER(1000, 1.05),
        GOLD(5000, 1.10),
        PLATINUM(10000, 1.15);

        private final int threshold;
        private final double discountMultiplier;

        LoyaltyTier(int threshold, double discountMultiplier) {
            this.threshold = threshold;
            this.discountMultiplier = discountMultiplier;
        }

        public int getThreshold() {
            return threshold;
        }

        public double getDiscountMultiplier() {
            return discountMultiplier;
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
        int pointsEarned = (int) (amount * 10); // 10 points per currency unit
        addLoyaltyPoints(pointsEarned);
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
}
