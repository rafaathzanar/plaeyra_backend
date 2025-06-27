package com.zanar.playera.controller;

import com.zanar.playera.dto.*;
import com.zanar.playera.service.OwnerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner-dashboard")
@CrossOrigin(origins = "*")
public class OwnerDashboardController {
    @Autowired
    private OwnerDashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary(
            @RequestParam Long ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return dashboardService.getDashboardSummary(ownerId, start, end);
    }
} 