package com.zanar.playera.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardSummaryDTO {
    private double totalRevenue;
    private int totalBookings;
    private int totalCancellations;
    private int totalEquipmentRentals;
    private List<KPIsDTO> kpis;
    private List<RevenueStatsDTO> revenueStats;
    private List<AlertDTO> alerts;
} 