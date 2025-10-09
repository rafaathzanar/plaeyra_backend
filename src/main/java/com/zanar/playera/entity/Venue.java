package com.zanar.playera.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "venueOwner", "courts", "reviews", "equipment" })
@Table(name = "venues")
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long venueId;

    @NotBlank(message = "Venue name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Address is required")
    @Column(nullable = false)
    private String address;

    @NotBlank(message = "Location is required")
    private String location;

    private String description;

    @NotBlank(message = "Contact number is required")
    private String contactNo;

    private String email;

    private String website;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    private VenueStatus status = VenueStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private VenueType venueType;

    private Integer maxCapacity;

    private Boolean parkingAvailable = false;

    private Boolean foodAvailable = false;

    private Boolean changingRoomsAvailable = false;

    private Boolean showerAvailable = false;

    private Boolean wifiAvailable = false;

    private String openingHours; // JSON string for business hours

    private String cancellationPolicy;

    private String refundPolicy;

    private Double basePrice;

    private Boolean dynamicPricingEnabled = false;

    private Double peakHourMultiplier = 1.5;

    private Double offPeakMultiplier = 0.8;

    private Double weekendMultiplier = 1.2;

    private Double holidayMultiplier = 1.3;

    private LocalTime peakHourStart = LocalTime.of(18, 0); // 6 PM

    private LocalTime peakHourEnd = LocalTime.of(22, 0); // 10 PM

    private String specialEvents; // JSON string for special event pricing

    private Double commissionRate = 0.10; // Default 10% commission

    private Boolean autoApprovalEnabled = false;

    private Integer minAdvanceBookingHours = 24;

    private Integer maxAdvanceBookingDays = 30;

    private LocalTime earliestBookingTime = LocalTime.of(6, 0); // 6 AM

    private LocalTime latestBookingTime = LocalTime.of(23, 0); // 11 PM

    @ElementCollection
    @CollectionTable(name = "venue_images", joinColumns = @JoinColumn(name = "venue_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "venue_amenities", joinColumns = @JoinColumn(name = "venue_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "venue_sports_types", joinColumns = @JoinColumn(name = "venue_id"))
    @Column(name = "sport_type")
    private List<String> sportsTypes = new ArrayList<>();

    @ElementCollection
    private Map<DayOfWeek, BusinessHours> businessHours = new HashMap<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Court> courts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private VenueOwner venueOwner;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    public enum VenueStatus {
        ACTIVE, INACTIVE, MAINTENANCE, SUSPENDED, DELETED
    }

    public enum VenueType {
        INDOOR, OUTDOOR, MIXED, SPECIALIZED
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessHours {
        private LocalTime openTime;
        private LocalTime closeTime;
        private Boolean isOpen;
        private String specialNotes;
    }

    // Helper methods for dynamic pricing
    public double calculateDynamicPrice(double basePrice, LocalTime time, DayOfWeek day) {
        if (!getDynamicPricingEnabled()) {
            return basePrice;
        }

        double multiplier = 1.0;

        // Peak hour pricing
        if (time.isAfter(peakHourStart) && time.isBefore(peakHourEnd)) {
            multiplier *= peakHourMultiplier;
        } else {
            multiplier *= offPeakMultiplier;
        }

        // Weekend pricing
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            multiplier *= weekendMultiplier;
        }

        return basePrice * multiplier;
    }

    public boolean isOpen(DayOfWeek day, LocalTime time) {
        BusinessHours hours = businessHours.get(day);
        if (hours == null || !hours.getIsOpen()) {
            return false;
        }

        return time.isAfter(hours.getOpenTime()) && time.isBefore(hours.getCloseTime());
    }

    public boolean canBookInAdvance(int hoursInAdvance) {
        return hoursInAdvance >= minAdvanceBookingHours;
    }

    public boolean isWithinBookingWindow(LocalTime time) {
        return time.isAfter(earliestBookingTime) && time.isBefore(latestBookingTime);
    }

    public void addCourt(Court court) {
        courts.add(court);
        court.setVenue(this);
    }

    public void removeCourt(Court court) {
        courts.remove(court);
        court.setVenue(null);
    }

    public double getAverageRating() {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }

    public int getTotalReviews() {
        return reviews.size();
    }
}
