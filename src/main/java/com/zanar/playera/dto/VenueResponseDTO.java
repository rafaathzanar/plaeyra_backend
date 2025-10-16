package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.List;

@Data
public class VenueResponseDTO {
  private Long venueId;
  private String name;
  private String address;
  private String location;
  private String description;
  private String contactNo;
  private String email;
  private String website;
  private Double latitude;
  private Double longitude;
  private String status;
  private String venueType;
  private Integer maxCapacity;
  private Boolean parkingAvailable;
  private Boolean foodAvailable;
  private Boolean changingRoomsAvailable;
  private Boolean showerAvailable;
  private Boolean wifiAvailable;
  private String openingHours;
  private String cancellationPolicy;
  private String refundPolicy;
  private Double basePrice;
  private Boolean dynamicPricingEnabled;
  private Double peakHourMultiplier;
  private Double offPeakMultiplier;
  private Double weekendMultiplier;
  private Double holidayMultiplier;
  private LocalTime peakHourStart;
  private LocalTime peakHourEnd;
  private String specialEvents;
  private Double commissionRate;
  private Boolean autoApprovalEnabled;
  private Integer minAdvanceBookingHours;
  private Integer maxAdvanceBookingDays;
  private LocalTime earliestBookingTime;
  private LocalTime latestBookingTime;
  private List<String> images;
  private List<String> amenities;
  private Long ownerId;
  private String ownerName;
  private List<CourtResponseDTO> courts;
}