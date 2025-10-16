package com.zanar.playera.service;

import com.zanar.playera.dto.AnalyticsResponseDTO;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Equipment;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.repo.BookingRepository;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.EquipmentRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnalyticsService {

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private CourtRepository courtRepository;

  @Autowired
  private EquipmentRepository equipmentRepository;

  @Autowired
  private VenueRepository venueRepository;

  public AnalyticsResponseDTO getVenueAnalytics(Long venueId, String dateRange) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    LocalDate endDate = LocalDate.now();
    LocalDate startDate = calculateStartDate(dateRange, endDate);

    // Get all bookings for the date range
    List<Booking> bookings = bookingRepository.findByVenueIdWithDetails(venueId);

    // Filter bookings by date range
    bookings = bookings.stream()
        .filter(b -> b.getBookingDate() != null &&
            b.getBookingDate().toLocalDate().isAfter(startDate.minusDays(1)) &&
            b.getBookingDate().toLocalDate().isBefore(endDate.plusDays(1)))
        .collect(Collectors.toList());

    // Get courts for this venue
    List<Court> courts = courtRepository.findAll().stream()
        .filter(c -> c.getVenue() != null && c.getVenue().getVenueId().equals(venueId))
        .collect(Collectors.toList());

    // Get equipment for this venue
    List<Equipment> equipment = equipmentRepository.findAll().stream()
        .filter(e -> e.getCourt() != null && e.getCourt().getVenue() != null &&
            e.getCourt().getVenue().getVenueId().equals(venueId))
        .collect(Collectors.toList());

    return buildAnalyticsResponse(venue, bookings, courts, equipment, startDate, endDate);
  }

  private LocalDate calculateStartDate(String dateRange, LocalDate endDate) {
    switch (dateRange.toLowerCase()) {
      case "week":
        return endDate.minusDays(7);
      case "month":
        return endDate.minusDays(30);
      case "quarter":
        return endDate.minusDays(90);
      case "year":
        return endDate.minusDays(365);
      default:
        return endDate.minusDays(30);
    }
  }

  private AnalyticsResponseDTO buildAnalyticsResponse(Venue venue, List<Booking> bookings,
      List<Court> courts, List<Equipment> equipment,
      LocalDate startDate, LocalDate endDate) {
    AnalyticsResponseDTO response = new AnalyticsResponseDTO();

    // Revenue calculations
    double totalRevenue = bookings.stream()
        .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
        .mapToDouble(Booking::getTotalCost)
        .sum();

    // Calculate equipment revenue from booking equipment
    double equipmentRevenue = bookings.stream()
        .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
        .flatMap(b -> b.getBookingEquipments() != null ? b.getBookingEquipments().stream() : Stream.empty())
        .mapToDouble(be -> be.getTotalPrice())
        .sum();

    double courtRevenue = totalRevenue - equipmentRevenue;

    response.setTotalRevenue(totalRevenue);
    response.setCourtRevenue(courtRevenue);
    response.setEquipmentRevenue(equipmentRevenue);

    // Booking statistics
    long totalBookings = bookings.size();
    long confirmedBookings = bookings.stream()
        .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
        .count();
    long pendingBookings = bookings.stream()
        .filter(b -> "PENDING".equals(b.getBookingStatus()))
        .count();
    long cancelledBookings = bookings.stream()
        .filter(b -> "CANCELLED".equals(b.getBookingStatus()))
        .count();

    response.setTotalBookings(totalBookings);
    response.setConfirmedBookings(confirmedBookings);
    response.setPendingBookings(pendingBookings);
    response.setCancelledBookings(cancelledBookings);

    // Customer statistics
    Set<Long> uniqueCustomers = bookings.stream()
        .filter(b -> b.getCustomer() != null)
        .map(b -> b.getCustomer().getUserId())
        .collect(Collectors.toSet());

    Set<Long> newCustomers = bookings.stream()
        .filter(b -> b.getCustomer() != null && b.getBookingDate() != null)
        .filter(b -> b.getBookingDate().toLocalDate().isAfter(startDate.plusDays(1)))
        .map(b -> b.getCustomer().getUserId())
        .collect(Collectors.toSet());

    response.setTotalCustomers(uniqueCustomers.size());
    response.setNewCustomers(newCustomers.size());
    response.setReturningCustomers(uniqueCustomers.size() - newCustomers.size());

    // Court occupancy calculations
    Map<Long, Double> courtOccupancy = new HashMap<>();
    Map<Long, Integer> courtBookings = new HashMap<>();
    Map<Long, Double> courtRevenueMap = new HashMap<>();

    for (Court court : courts) {
      List<Booking> courtBookingList = bookings.stream()
          .filter(b -> b.getBookingCourts() != null)
          .filter(b -> b.getBookingCourts().stream()
              .anyMatch(bc -> bc.getCourt().getCourtId().equals(court.getCourtId())))
          .collect(Collectors.toList());

      int bookingsCount = courtBookingList.size();
      double revenue = courtBookingList.stream()
          .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
          .mapToDouble(Booking::getTotalCost)
          .sum();

      // Calculate occupancy percentage (simplified - based on booking count vs
      // available slots)
      double occupancy = bookingsCount > 0 ? Math.min(100.0, (bookingsCount * 10.0)) : 0.0;

      courtOccupancy.put(court.getCourtId(), occupancy);
      courtBookings.put(court.getCourtId(), bookingsCount);
      courtRevenueMap.put(court.getCourtId(), revenue);
    }

    response.setCourtOccupancy(courtOccupancy);
    response.setCourtBookings(courtBookings);
    response.setCourtRevenueMap(courtRevenueMap);

    // Equipment usage
    Map<Long, Integer> equipmentUsage = new HashMap<>();
    Map<Long, Double> equipmentRevenueMap = new HashMap<>();

    for (Equipment eq : equipment) {
      int usageCount = (int) bookings.stream()
          .filter(b -> b.getBookingEquipments() != null)
          .flatMap(b -> b.getBookingEquipments().stream())
          .filter(be -> be.getEquipment().getEquipmentId().equals(eq.getEquipmentId()))
          .count();

      double eqRevenue = bookings.stream()
          .filter(b -> b.getBookingEquipments() != null)
          .flatMap(b -> b.getBookingEquipments().stream())
          .filter(be -> be.getEquipment().getEquipmentId().equals(eq.getEquipmentId()))
          .mapToDouble(be -> be.getTotalPrice())
          .sum();

      equipmentUsage.put(eq.getEquipmentId(), usageCount);
      equipmentRevenueMap.put(eq.getEquipmentId(), eqRevenue);
    }

    response.setEquipmentUsage(equipmentUsage);
    response.setEquipmentRevenueMap(equipmentRevenueMap);

    // Time slot analysis
    Map<String, Integer> hourlyBookings = new HashMap<>();
    for (Booking booking : bookings) {
      if (booking.getBookingTimeSlots() != null) {
        booking.getBookingTimeSlots().forEach(slot -> {
          if (slot.getStartTime() != null) {
            String hour = slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            hourlyBookings.put(hour, hourlyBookings.getOrDefault(hour, 0) + 1);
          }
        });
      }
    }

    // Find peak hours (top 3)
    List<String> peakHours = hourlyBookings.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(3)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    // Find off-peak hours (bottom 3)
    List<String> offPeakHours = hourlyBookings.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .limit(3)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    response.setPeakHours(peakHours);
    response.setOffPeakHours(offPeakHours);

    // Monthly trends (last 6 months)
    Map<String, Double> monthlyTrends = new HashMap<>();
    for (int i = 5; i >= 0; i--) {
      LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
      LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

      double monthRevenue = bookings.stream()
          .filter(b -> b.getBookingDate() != null)
          .filter(b -> b.getBookingDate().toLocalDate().isAfter(monthStart.minusDays(1)) &&
              b.getBookingDate().toLocalDate().isBefore(monthEnd.plusDays(1)))
          .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
          .mapToDouble(Booking::getTotalCost)
          .sum();

      String monthKey = monthStart.format(DateTimeFormatter.ofPattern("MMM"));
      monthlyTrends.put(monthKey, monthRevenue);
    }

    response.setMonthlyTrends(monthlyTrends);

    // Calculate trends (simplified - comparing with previous period)
    LocalDate previousStart = startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay());
    LocalDate previousEnd = startDate;

    List<Booking> previousBookings = bookingRepository.findByVenueIdWithDetails(venue.getVenueId());
    previousBookings = previousBookings.stream()
        .filter(b -> b.getBookingDate() != null)
        .filter(b -> b.getBookingDate().toLocalDate().isAfter(previousStart.minusDays(1)) &&
            b.getBookingDate().toLocalDate().isBefore(previousEnd.plusDays(1)))
        .collect(Collectors.toList());

    double previousRevenue = previousBookings.stream()
        .filter(b -> "CONFIRMED".equals(b.getBookingStatus()))
        .mapToDouble(Booking::getTotalCost)
        .sum();

    double revenueChange = previousRevenue > 0 ? ((totalRevenue - previousRevenue) / previousRevenue) * 100 : 0;

    response.setRevenueChange(revenueChange);
    response.setRevenueTrend(revenueChange > 0 ? "up" : revenueChange < 0 ? "down" : "neutral");

    return response;
  }
}
