package com.zanar.playera.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("VENUE_OWNER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueOwner extends User {

    private String businessName;

    private String businessRegistrationNumber;

    private String businessAddress;

    private String businessPhone;

    private String businessEmail;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    private LocalDateTime verificationDate;

    private String verificationDocuments; // Comma-separated list of document URLs

    private String bankAccountNumber;

    private String bankName;

    private String bankBranch;

    private Double commissionRate = 0.10; // Default 10% commission

    private Boolean autoApprovalEnabled = false;

    private String cancellationPolicy;

    private String refundPolicy;

    private LocalDateTime businessStartedAt;

    private Integer totalVenues = 0;

    private Double totalRevenue = 0.0;

    private Integer totalBookings = 0;

    @OneToMany(mappedBy = "venueOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Venue> venues = new ArrayList<>();

    // Reviews are accessed through venues, not directly
    // @OneToMany(mappedBy = "venueOwner", cascade = CascadeType.ALL, orphanRemoval
    // = true)
    // private List<Review> reviews = new ArrayList<>();

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, SUSPENDED
    }

    // Helper methods
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    public boolean canAddVenue() {
        return isVerified() && getStatus() == UserStatus.ACTIVE;
    }

    public void addVenue() {
        this.totalVenues++;
    }

    public void removeVenue() {
        if (this.totalVenues > 0) {
            this.totalVenues--;
        }
    }

    public void addRevenue(double amount) {
        this.totalRevenue += amount;
    }

    public void addBooking() {
        this.totalBookings++;
    }

    public double calculateCommission(double amount) {
        return amount * commissionRate;
    }

    public void updateVerificationStatus(VerificationStatus status) {
        this.verificationStatus = status;
        if (status == VerificationStatus.VERIFIED) {
            this.verificationDate = LocalDateTime.now();
        }
    }
}
