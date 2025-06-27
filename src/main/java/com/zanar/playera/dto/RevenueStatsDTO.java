package com.zanar.playera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RevenueStatsDTO {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double totalRevenue;
    private double totalProfit;
    private int bookingCount;
} 