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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
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
    log.info("=== ANALYTICS CALCULATION DEBUG ===");
    log.info("Calculating analytics for venue ID: {}, date range: {}", venueId, dateRange);

    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    LocalDate endDate = LocalDate.now();
    LocalDate startDate = calculateStartDate(dateRange, endDate);

    log.info("Date range: {} to {}", startDate, endDate);
    log.info("Current date: {}", LocalDate.now());
    log.info("Selected date range: {}", dateRange);

    // Get all bookings for the date range
    List<Booking> bookings = bookingRepository.findByVenueIdWithDetails(venueId);
    log.info("Total bookings found: {}", bookings.size());

    // Load missing relationships to avoid MultipleBagFetchException
    for (Booking booking : bookings) {
      // Load equipment relationships
      Booking bookingWithEquipment = bookingRepository.findByIdWithEquipment(booking.getBookingId());
      if (bookingWithEquipment != null && bookingWithEquipment.getBookingEquipments() != null) {
        booking.setBookingEquipments(bookingWithEquipment.getBookingEquipments());
      }

      // Load time slot relationships
      Booking bookingWithTimeSlots = bookingRepository.findByIdWithTimeSlots(booking.getBookingId());
      if (bookingWithTimeSlots != null && bookingWithTimeSlots.getBookingTimeSlots() != null) {
        booking.setBookingTimeSlots(bookingWithTimeSlots.getBookingTimeSlots());
      }

      // Load court relationships
      Booking bookingWithCourts = bookingRepository.findByIdWithCourts(booking.getBookingId());
      if (bookingWithCourts != null && bookingWithCourts.getBookingCourts() != null) {
        booking.setBookingCourts(bookingWithCourts.getBookingCourts());
      }
    }

    // Filter bookings by date range - but include ALL cancelled bookings regardless
    // of date
    log.info("=== DATE FILTERING DEBUG ===");
    List<Booking> beforeFilter = new ArrayList<>(bookings);
    for (Booking booking : beforeFilter) {
      if (booking.getBookingDate() != null) {
        LocalDate bookingDate = booking.getBookingDate().toLocalDate();
        boolean isAfterStart = bookingDate.isAfter(startDate.minusDays(1));
        boolean isBeforeEnd = bookingDate.isBefore(endDate.plusDays(1));
        boolean isCancelled = booking.getBookingStatus() == Booking.BookingStatus.CANCELLED;
        boolean passesFilter = (isAfterStart && isBeforeEnd) || isCancelled;
        log.info("Booking {}: Date={}, Status={}, After Start={}, Before End={}, Is Cancelled={}, Passes Filter={}",
            booking.getBookingId(), bookingDate, booking.getBookingStatus(), isAfterStart, isBeforeEnd, isCancelled,
            passesFilter);
      } else {
        log.info("Booking {}: Date is NULL", booking.getBookingId());
      }
    }

    bookings = bookings.stream()
        .filter(b -> {
          if (b.getBookingDate() == null)
            return false;

          LocalDate bookingDate = b.getBookingDate().toLocalDate();
          boolean inDateRange = bookingDate.isAfter(startDate.minusDays(1)) &&
              bookingDate.isBefore(endDate.plusDays(1));
          boolean isCancelled = b.getBookingStatus() == Booking.BookingStatus.CANCELLED;

          return inDateRange || isCancelled;
        })
        .collect(Collectors.toList());
    log.info("=== END DATE FILTERING DEBUG ===");

    log.info("Filtered bookings for date range: {}", bookings.size());

    // Log booking details for debugging
    log.info("=== DETAILED BOOKING DEBUG ===");
    for (Booking booking : bookings) {
      log.info("Booking ID: {}, Status: '{}', Total Cost: {}, Date: {}, Equipment Count: {}, Time Slots Count: {}",
          booking.getBookingId(),
          booking.getBookingStatus(),
          booking.getTotalCost(),
          booking.getBookingDate(),
          booking.getBookingEquipments() != null ? booking.getBookingEquipments().size() : 0,
          booking.getBookingTimeSlots() != null ? booking.getBookingTimeSlots().size() : 0);

      // Check if status matches BOOKED
      String actualStatus = booking.getBookingStatus() != null ? booking.getBookingStatus().toString() : "NULL";
      boolean isBooked = booking.getBookingStatus() == Booking.BookingStatus.BOOKED;
      log.info("  -> Actual Status: '{}', Is BOOKED status? {}", actualStatus, isBooked);

      // Log equipment details if any
      if (booking.getBookingEquipments() != null && !booking.getBookingEquipments().isEmpty()) {
        for (var be : booking.getBookingEquipments()) {
          log.info("  -> Equipment: {}, Price: {}, Status: {}",
              be.getEquipment() != null ? be.getEquipment().getName() : "Unknown",
              be.getTotalPrice(),
              be.getStatus());
        }
      }
    }
    log.info("=== END DETAILED BOOKING DEBUG ===");

    // Get courts for this venue
    List<Court> courts = courtRepository.findAll().stream()
        .filter(c -> c.getVenue() != null && c.getVenue().getVenueId().equals(venueId))
        .collect(Collectors.toList());

    log.info("Courts found: {}", courts.size());

    // Get equipment for this venue
    List<Equipment> equipment = equipmentRepository.findAll().stream()
        .filter(e -> e.getCourt() != null && e.getCourt().getVenue() != null &&
            e.getCourt().getVenue().getVenueId().equals(venueId))
        .collect(Collectors.toList());

    log.info("Equipment found: {}", equipment.size());

    AnalyticsResponseDTO response = buildAnalyticsResponse(venue, bookings, courts, equipment, startDate, endDate);

    log.info("Analytics response - Total Revenue: {}, Total Bookings: {}, Total Customers: {}",
        response.getTotalRevenue(), response.getTotalBookings(), response.getTotalCustomers());
    log.info("=== END ANALYTICS CALCULATION DEBUG ===");

    return response;
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
    log.info("Building analytics response for {} bookings, {} courts, {} equipment",
        bookings.size(), courts.size(), equipment.size());

    AnalyticsResponseDTO response = new AnalyticsResponseDTO();

    // Filter bookings by confirmed status (BOOKED)
    List<Booking> confirmedBookings = bookings.stream()
        .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
        .collect(Collectors.toList());

    log.info("Confirmed bookings: {}", confirmedBookings.size());

    // Revenue calculations - use confirmed bookings only
    double totalRevenue = confirmedBookings.stream()
        .mapToDouble(Booking::getTotalCost)
        .sum();

    // Calculate equipment revenue from booking equipment (only confirmed bookings)
    double equipmentRevenue = confirmedBookings.stream()
        .flatMap(b -> b.getBookingEquipments() != null ? b.getBookingEquipments().stream() : Stream.empty())
        .mapToDouble(be -> be.getTotalPrice())
        .sum();

    // Court revenue is the total revenue minus equipment revenue
    double courtRevenue = totalRevenue - equipmentRevenue;

    log.info("Revenue - Total: {}, Court: {}, Equipment: {}", totalRevenue, courtRevenue, equipmentRevenue);

    response.setTotalRevenue(totalRevenue);
    response.setCourtRevenue(courtRevenue);
    response.setEquipmentRevenue(equipmentRevenue);

    // Booking statistics
    long totalBookings = bookings.size();
    long confirmedBookingsCount = bookings.stream()
        .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
        .count();
    long pendingBookings = 0; // No PENDING status in enum
    long cancelledBookings = bookings.stream()
        .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
        .count();

    log.info("Booking status counts - Total: {}, Confirmed: {}, Pending: {}, Cancelled: {}",
        totalBookings, confirmedBookingsCount, pendingBookings, cancelledBookings);

    // Log all booking statuses for debugging
    Map<String, Long> statusCounts = bookings.stream()
        .collect(Collectors.groupingBy(
            b -> b.getBookingStatus() != null ? b.getBookingStatus().toString() : "NULL",
            Collectors.counting()));
    log.info("All booking statuses: {}", statusCounts);

    response.setTotalBookings(totalBookings);
    response.setConfirmedBookings(confirmedBookingsCount);
    response.setPendingBookings(pendingBookings);
    response.setCancelledBookings(cancelledBookings);

    // Customer statistics - fixed logic
    Set<Long> uniqueCustomers = bookings.stream()
        .filter(b -> b.getCustomer() != null)
        .map(b -> b.getCustomer().getUserId())
        .collect(Collectors.toSet());

    // Count customers by number of bookings
    Map<Long, Long> customerBookingCounts = bookings.stream()
        .filter(b -> b.getCustomer() != null)
        .collect(Collectors.groupingBy(
            b -> b.getCustomer().getUserId(),
            Collectors.counting()));

    // New customers are those with only 1 booking
    long newCustomers = customerBookingCounts.values().stream()
        .mapToLong(count -> count == 1 ? 1 : 0)
        .sum();

    // Returning customers are those with 2+ bookings
    long returningCustomers = customerBookingCounts.values().stream()
        .mapToLong(count -> count >= 2 ? 1 : 0)
        .sum();

    log.info("Customer analysis - Total unique: {}, New (1 booking): {}, Returning (2+ bookings): {}",
        uniqueCustomers.size(), newCustomers, returningCustomers);

    response.setTotalCustomers(uniqueCustomers.size());
    response.setNewCustomers((int) newCustomers);
    response.setReturningCustomers((int) returningCustomers);

    // Court occupancy calculations - improved accuracy
    Map<Long, Double> courtOccupancy = new HashMap<>();
    Map<Long, Integer> courtBookings = new HashMap<>();
    Map<Long, Double> courtRevenueMap = new HashMap<>();

    for (Court court : courts) {
      // Get bookings for this specific court
      List<Booking> courtBookingList = bookings.stream()
          .filter(b -> b.getBookingCourts() != null)
          .filter(b -> b.getBookingCourts().stream()
              .anyMatch(bc -> bc.getCourt().getCourtId().equals(court.getCourtId())))
          .collect(Collectors.toList());

      int bookingsCount = courtBookingList.size();

      // Calculate revenue for this court (only confirmed bookings)
      double revenue = courtBookingList.stream()
          .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
          .mapToDouble(Booking::getTotalCost)
          .sum();

      // Calculate realistic occupancy percentage based on actual court operating
      // hours
      // Get court operating hours
      LocalTime openingTime = court.getOpeningTime();
      LocalTime closingTime = court.getClosingTime();

      // Calculate operating hours per day
      long operatingMinutes = java.time.temporal.ChronoUnit.MINUTES.between(openingTime, closingTime);
      int slotsPerDay = (int) (operatingMinutes / 30); // 30-minute slots

      // Calculate total possible slots for the date range
      int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
      int totalPossibleSlots = totalDays * slotsPerDay;

      // Calculate actual booked slots for this court based on time slots
      int bookedSlots = 0;
      for (Booking booking : courtBookingList) {
        if (booking.getBookingStatus() == Booking.BookingStatus.BOOKED && booking.getBookingTimeSlots() != null) {
          for (var timeSlot : booking.getBookingTimeSlots()) {
            // Check if this time slot is for this court
            if (booking.getBookingCourts() != null) {
              boolean isForThisCourt = booking.getBookingCourts().stream()
                  .anyMatch(bc -> bc.getCourt().getCourtId().equals(court.getCourtId()));
              if (isForThisCourt) {
                // Each time slot represents 30 minutes
                bookedSlots += 1;
              }
            }
          }
        }
      }

      double occupancy = totalPossibleSlots > 0 ? Math.min(100.0, (bookedSlots * 100.0) / totalPossibleSlots) : 0.0;

      log.info(
          "Court {} - Operating hours: {} to {}, Slots per day: {}, Total days: {}, Total possible slots: {}, Booked slots: {}, Occupancy: {}%",
          court.getCourtName(), openingTime, closingTime, slotsPerDay, totalDays, totalPossibleSlots, bookedSlots,
          occupancy);

      courtOccupancy.put(court.getCourtId(), occupancy);
      courtBookings.put(court.getCourtId(), bookingsCount);
      courtRevenueMap.put(court.getCourtId(), revenue);
    }

    response.setCourtOccupancy(courtOccupancy);
    response.setCourtBookings(courtBookings);
    response.setCourtRevenueMap(courtRevenueMap);

    // Court names mapping
    Map<Long, String> courtNames = courts.stream()
        .collect(Collectors.toMap(Court::getCourtId, Court::getCourtName));
    response.setCourtNames(courtNames);

    // Equipment usage - improved calculations
    Map<Long, Integer> equipmentUsage = new HashMap<>();
    Map<Long, Double> equipmentRevenueMap = new HashMap<>();

    for (Equipment eq : equipment) {
      // Count equipment usage from confirmed bookings only
      int usageCount = (int) confirmedBookings.stream()
          .filter(b -> b.getBookingEquipments() != null)
          .flatMap(b -> b.getBookingEquipments().stream())
          .filter(be -> be.getEquipment().getEquipmentId().equals(eq.getEquipmentId()))
          .count();

      // Calculate equipment revenue from confirmed bookings only
      double eqRevenue = confirmedBookings.stream()
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

    // Equipment names mapping
    Map<Long, String> equipmentNames = equipment.stream()
        .collect(Collectors.toMap(Equipment::getEquipmentId, Equipment::getName));
    response.setEquipmentNames(equipmentNames);

    // Time slot analysis
    Map<String, Integer> hourlyBookings = new HashMap<>();
    for (Booking booking : confirmedBookings) { // Only confirmed bookings for time slot analysis
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

    // Monthly trends (last 6 months) - improved calculation
    Map<String, Double> monthlyTrends = new HashMap<>();
    for (int i = 5; i >= 0; i--) {
      LocalDate monthStart = endDate.minusMonths(i).withDayOfMonth(1);
      LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

      // Get all bookings for this venue (not just filtered ones)
      List<Booking> allBookings = bookingRepository.findByVenueIdWithDetails(venue.getVenueId());

      double monthRevenue = allBookings.stream()
          .filter(b -> b.getBookingDate() != null)
          .filter(b -> b.getBookingDate().toLocalDate().isAfter(monthStart.minusDays(1)) &&
              b.getBookingDate().toLocalDate().isBefore(monthEnd.plusDays(1)))
          .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
          .mapToDouble(Booking::getTotalCost)
          .sum();

      String monthKey = monthStart.format(DateTimeFormatter.ofPattern("MMM"));
      monthlyTrends.put(monthKey, monthRevenue);
    }

    response.setMonthlyTrends(monthlyTrends);

    // Calculate revenue trends - improved calculation
    LocalDate previousStart = startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay());
    LocalDate previousEnd = startDate;

    // Get all bookings for comparison
    List<Booking> allBookings = bookingRepository.findByVenueIdWithDetails(venue.getVenueId());
    List<Booking> previousBookings = allBookings.stream()
        .filter(b -> b.getBookingDate() != null)
        .filter(b -> b.getBookingDate().toLocalDate().isAfter(previousStart.minusDays(1)) &&
            b.getBookingDate().toLocalDate().isBefore(previousEnd.plusDays(1)))
        .collect(Collectors.toList());

    double previousRevenue = previousBookings.stream()
        .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
        .mapToDouble(Booking::getTotalCost)
        .sum();

    double revenueChange = previousRevenue > 0 ? ((totalRevenue - previousRevenue) / previousRevenue) * 100 : 0;

    response.setRevenueChange(revenueChange);
    response.setRevenueTrend(revenueChange > 0 ? "up" : revenueChange < 0 ? "down" : "neutral");

    return response;
  }
}
