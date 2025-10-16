package com.zanar.playera.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsResponseDTO {
  // Revenue metrics
  private Double totalRevenue;
  private Double courtRevenue;
  private Double equipmentRevenue;
  private Double revenueChange;
  private String revenueTrend;

  // Booking metrics
  private Long totalBookings;
  private Long confirmedBookings;
  private Long pendingBookings;
  private Long cancelledBookings;

  // Customer metrics
  private Integer totalCustomers;
  private Integer newCustomers;
  private Integer returningCustomers;

  // Court metrics
  private Map<Long, Double> courtOccupancy;
  private Map<Long, Integer> courtBookings;
  private Map<Long, Double> courtRevenueMap;

  // Equipment metrics
  private Map<Long, Integer> equipmentUsage;
  private Map<Long, Double> equipmentRevenueMap;

  // Time analysis
  private List<String> peakHours;
  private List<String> offPeakHours;

  // Trends
  private Map<String, Double> monthlyTrends;
}
