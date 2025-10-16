package com.zanar.playera.service;

import com.zanar.playera.dto.*;
import com.zanar.playera.entity.*;
import com.zanar.playera.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OwnerDashboardService {
        @Autowired
        private VenueRepository venueRepository;
        @Autowired
        private BookingRepository bookingRepository;
        @Autowired
        private EquipmentRepository equipmentRepository;
        @Autowired
        private PaymentRepository paymentRepository;

        public DashboardSummaryDTO getDashboardSummary(Long ownerId, LocalDate start, LocalDate end) {
                // Each owner has only one venue
                Venue venue = venueRepository.findAll().stream()
                                .filter(v -> v.getVenueOwner() != null && v.getVenueOwner().getUserId().equals(ownerId))
                                .findFirst()
                                .orElse(null); // Return null instead of throwing exception

                // If no venue exists, return empty dashboard data
                if (venue == null) {
                        DashboardSummaryDTO emptySummary = new DashboardSummaryDTO();
                        emptySummary.setTotalRevenue(0.0);
                        emptySummary.setTotalRefunds(0.0);
                        emptySummary.setNetRevenue(0.0);
                        emptySummary.setTotalBookings(0);
                        emptySummary.setTotalCancellations(0);
                        emptySummary.setTotalEquipmentRentals(0);
                        emptySummary.setKpis(new ArrayList<>());
                        emptySummary.setRevenueStats(new ArrayList<>());
                        emptySummary.setAlerts(new ArrayList<>());
                        return emptySummary;
                }

                List<Long> venueIds = List.of(venue.getVenueId());
                List<Booking> bookings = bookingRepository.findAll().stream()
                                .filter(b -> b.getBookingDate() != null &&
                                                !b.getBookingDate().toLocalDate().isBefore(start) &&
                                                !b.getBookingDate().toLocalDate().isAfter(end) &&
                                                b.getBookingCourts() != null &&
                                                b.getBookingCourts().stream()
                                                                .anyMatch(bc -> bc.getCourt().getVenue() != null
                                                                                && venueIds.contains(bc.getCourt()
                                                                                                .getVenue()
                                                                                                .getVenueId())))
                                .collect(Collectors.toList());
                double totalRevenue = bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
                                .mapToDouble(Booking::getTotalCost)
                                .sum();

                // Calculate total refunds from cancelled bookings
                double totalRefunds = bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
                                .filter(b -> b.getPayment() != null && b.getPayment().getRefundAmount() != null)
                                .mapToDouble(b -> b.getPayment().getRefundAmount())
                                .sum();

                double netRevenue = totalRevenue - totalRefunds;

                int totalBookings = bookings.size();
                int totalCancellations = (int) bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
                                .count();
                int totalEquipmentRentals = bookings.stream()
                                .mapToInt(b -> b.getBookingEquipments() != null ? b.getBookingEquipments().size() : 0)
                                .sum();

                KPIsDTO kpis = calculateKPIs(bookings, List.of(venue));
                RevenueStatsDTO revenueStats = calculateRevenueStats(bookings, start, end);
                List<AlertDTO> alerts = generateAlerts(bookings, List.of(venue));

                DashboardSummaryDTO summary = new DashboardSummaryDTO();
                summary.setTotalRevenue(totalRevenue);
                summary.setTotalRefunds(totalRefunds);
                summary.setNetRevenue(netRevenue);
                summary.setTotalBookings(totalBookings);
                summary.setTotalCancellations(totalCancellations);
                summary.setTotalEquipmentRentals(totalEquipmentRentals);
                summary.setKpis(List.of(kpis));
                summary.setRevenueStats(List.of(revenueStats));
                summary.setAlerts(alerts);
                return summary;
        }

        public KPIsDTO calculateKPIs(List<Booking> bookings, List<Venue> venues) {
                KPIsDTO kpis = new KPIsDTO();
                double totalRevenue = bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
                                .mapToDouble(Booking::getTotalCost)
                                .sum();
                int confirmedBookings = (int) bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
                                .count();
                kpis.setAverageBookingValue(confirmedBookings == 0 ? 0 : totalRevenue / confirmedBookings);
                // Occupancy rate: booked slots / total slots
                int totalSlots = venues.stream().flatMap(v -> v.getCourts().stream()).mapToInt(c -> c.getSlots().size())
                                .sum();
                int bookedSlots = venues.stream().flatMap(v -> v.getCourts().stream())
                                .mapToInt(c -> (int) c.getSlots().stream()
                                                .filter(s -> s.getStatus().name().equals("BOOKED")).count())
                                .sum();
                kpis.setOccupancyRate(totalSlots == 0 ? 0 : (double) bookedSlots / totalSlots);
                // Equipment utilization: rented hours / available hours
                int rentedHours = bookings.stream()
                                .flatMap(b -> b.getBookingEquipments() != null ? b.getBookingEquipments().stream()
                                                : new ArrayList<BookingEquipment>().stream())
                                .mapToInt(BookingEquipment::getTimeDuration).sum();
                int availableHours = venues.stream().flatMap(v -> v.getCourts().stream())
                                .mapToInt(c -> c.getSlots().size())
                                .sum();
                kpis.setEquipmentUtilization(availableHours == 0 ? 0 : (double) rentedHours / availableHours);
                // Profit margin: (revenue - cost) / revenue (cost not tracked, so set to 0)
                kpis.setProfitMargin(1.0); // Placeholder
                return kpis;
        }

        public RevenueStatsDTO calculateRevenueStats(List<Booking> bookings, LocalDate start, LocalDate end) {
                RevenueStatsDTO stats = new RevenueStatsDTO();
                stats.setPeriodStart(start);
                stats.setPeriodEnd(end);
                stats.setTotalRevenue(bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
                                .mapToDouble(Booking::getTotalCost)
                                .sum());
                stats.setTotalProfit(stats.getTotalRevenue()); // Placeholder
                stats.setBookingCount((int) bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.BOOKED)
                                .count());
                return stats;
        }

        public List<AlertDTO> generateAlerts(List<Booking> bookings, List<Venue> venues) {
                List<AlertDTO> alerts = new ArrayList<>();
                // Overbooking alert
                long overbooked = bookings.stream()
                                .filter(b -> b.getBookingCourts() != null && b.getBookingCourts().size() > 1)
                                .count();
                if (overbooked > 0) {
                        AlertDTO alert = new AlertDTO();
                        alert.setType("OVERBOOKING");
                        alert.setMessage("There are " + overbooked + " overbooked courts.");
                        alert.setTimestamp(java.time.LocalDateTime.now());
                        alerts.add(alert);
                }
                // High cancellation rate alert
                long cancellations = bookings.stream()
                                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CANCELLED).count();
                if (bookings.size() > 0 && ((double) cancellations / bookings.size()) > 0.3) {
                        AlertDTO alert = new AlertDTO();
                        alert.setType("HIGH_CANCELLATION_RATE");
                        alert.setMessage("High cancellation rate detected: " + (100 * cancellations / bookings.size())
                                        + "%");
                        alert.setTimestamp(java.time.LocalDateTime.now());
                        alerts.add(alert);
                }
                return alerts;
        }
}