package com.zanar.playera.service;

import com.zanar.playera.dto.VenueRequestDTO;
import com.zanar.playera.dto.VenueResponseDTO;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Favorite;
import com.zanar.playera.entity.Review;
import com.zanar.playera.mapper.VenueMapper;
import com.zanar.playera.repo.VenueRepository;
import com.zanar.playera.repo.VenueOwnerRepository;
import com.zanar.playera.repo.CustomerRepository;
import com.zanar.playera.repo.FavoriteRepository;
import com.zanar.playera.repo.ReviewRepository;
import com.zanar.playera.repo.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class VenueService {

  @Autowired
  private VenueRepository venueRepository;

  @Autowired
  private VenueOwnerRepository venueOwnerRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private FavoriteRepository favoriteRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private VenueMapper venueMapper;

  /**
   * Create a new venue
   */
  public VenueResponseDTO createVenue(VenueRequestDTO venueRequestDTO) {
    VenueOwner owner = venueOwnerRepository.findById(venueRequestDTO.getOwnerId())
        .orElseThrow(() -> new RuntimeException("Venue owner not found"));

    if (!owner.canAddVenue()) {
      throw new RuntimeException("Venue owner is not verified or active, or already has a venue");
    }

    // Check if owner already has a venue
    if (owner.hasVenue()) {
      throw new RuntimeException("Venue owner can only have one venue");
    }

    Venue venue = VenueMapper.toVenueEntity(venueRequestDTO, owner);
    venue.setStatus(Venue.VenueStatus.ACTIVE);

    Venue savedVenue = venueRepository.save(venue);
    owner.setVenue(savedVenue);
    venueOwnerRepository.save(owner);

    return VenueMapper.toVenueResponseDTO(savedVenue);
  }

  /**
   * Get venue by ID
   */
  public VenueResponseDTO getVenueById(Long id) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));
    return VenueMapper.toVenueResponseDTO(venue);
  }

  /**
   * Get all venues with filtering and pagination
   */
  public Page<VenueResponseDTO> getAllVenues(String location, String sportType, String venueType,
      Double minPrice, Double maxPrice, Boolean hasParking,
      Boolean hasFood, Boolean hasChangingRooms,
      Boolean hasShower, Boolean hasWifi, Pageable pageable) {

    List<Venue> allVenues = venueRepository.findAll();

    // Apply filters
    List<Venue> filteredVenues = allVenues.stream()
        .filter(venue -> location == null || venue.getLocation().toLowerCase().contains(location.toLowerCase()))
        .filter(venue -> sportType == null || venue.getSportsTypes().contains(sportType))
        .filter(venue -> venueType == null || venue.getVenueType().name().equals(venueType))
        .filter(venue -> minPrice == null || venue.getBasePrice() >= minPrice)
        .filter(venue -> maxPrice == null || venue.getBasePrice() <= maxPrice)
        .filter(venue -> hasParking == null || venue.getParkingAvailable() == hasParking)
        .filter(venue -> hasFood == null || venue.getFoodAvailable() == hasFood)
        .filter(venue -> hasChangingRooms == null || venue.getChangingRoomsAvailable() == hasChangingRooms)
        .filter(venue -> hasShower == null || venue.getShowerAvailable() == hasShower)
        .filter(venue -> hasWifi == null || venue.getWifiAvailable() == hasWifi)
        .filter(venue -> venue.getStatus() == Venue.VenueStatus.ACTIVE)
        .collect(Collectors.toList());

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), filteredVenues.size());

    if (start > filteredVenues.size()) {
      return new PageImpl<>(new ArrayList<>(), pageable, filteredVenues.size());
    }

    List<Venue> pageContent = filteredVenues.subList(start, end);
    List<VenueResponseDTO> venueDTOs = pageContent.stream()
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());

    return new PageImpl<>(venueDTOs, pageable, filteredVenues.size());
  }

  /**
   * Search venues by query
   */
  public List<VenueResponseDTO> searchVenues(String query, String location, String sportType) {
    List<Venue> allVenues = venueRepository.findAll();

    return allVenues.stream()
        .filter(venue -> venue.getStatus() == Venue.VenueStatus.ACTIVE)
        .filter(venue -> query == null ||
            venue.getName().toLowerCase().contains(query.toLowerCase()) ||
            venue.getDescription().toLowerCase().contains(query.toLowerCase()) ||
            venue.getAddress().toLowerCase().contains(query.toLowerCase()))
        .filter(venue -> location == null || venue.getLocation().toLowerCase().contains(location.toLowerCase()))
        .filter(venue -> sportType == null || venue.getSportsTypes().contains(sportType))
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Find nearby venues
   */
  public List<VenueResponseDTO> getNearbyVenues(Double latitude, Double longitude, Double radiusKm) {
    List<Venue> allVenues = venueRepository.findAll();

    return allVenues.stream()
        .filter(venue -> venue.getStatus() == Venue.VenueStatus.ACTIVE)
        .filter(venue -> calculateDistance(latitude, longitude, venue.getLatitude(), venue.getLongitude()) <= radiusKm)
        .sorted(Comparator.comparingDouble(
            venue -> calculateDistance(latitude, longitude, venue.getLatitude(), venue.getLongitude())))
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get venue by owner ID (each owner can have only one venue)
   */
  public VenueResponseDTO getVenueByOwner(Long ownerId) {
    Venue venue = venueRepository.findAll().stream()
        .filter(v -> v.getVenueOwner() != null && v.getVenueOwner().getUserId().equals(ownerId))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Venue not found for owner"));
    return VenueMapper.toVenueResponseDTO(venue);
  }

  /**
   * Get venues by owner (legacy method - returns list with single venue)
   */
  public List<VenueResponseDTO> getVenuesByOwner(Long ownerId) {
    try {
      VenueResponseDTO venue = getVenueByOwner(ownerId);
      return List.of(venue);
    } catch (RuntimeException e) {
      return new ArrayList<>();
    }
  }

  /**
   * Update venue
   */
  public VenueResponseDTO updateVenue(Long id, VenueRequestDTO venueRequestDTO) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    // Update venue fields from DTO
    if (venueRequestDTO.getName() != null)
      venue.setName(venueRequestDTO.getName());
    if (venueRequestDTO.getAddress() != null)
      venue.setAddress(venueRequestDTO.getAddress());
    if (venueRequestDTO.getLocation() != null)
      venue.setLocation(venueRequestDTO.getLocation());
    if (venueRequestDTO.getDescription() != null)
      venue.setDescription(venueRequestDTO.getDescription());
    if (venueRequestDTO.getContactNo() != null)
      venue.setContactNo(venueRequestDTO.getContactNo());

    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Delete venue
   */
  public void deleteVenue(Long id) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.setStatus(Venue.VenueStatus.DELETED);
    venueRepository.save(venue);
  }

  /**
   * Update venue status
   */
  public VenueResponseDTO updateVenueStatus(Long id, String status) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.setStatus(Venue.VenueStatus.valueOf(status.toUpperCase()));
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Add venue images
   */
  public VenueResponseDTO addVenueImages(Long id, List<String> imageUrls) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getImages().addAll(imageUrls);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Remove venue images
   */
  public VenueResponseDTO removeVenueImages(Long id, List<String> imageUrls) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getImages().removeAll(imageUrls);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Add venue amenities
   */
  public VenueResponseDTO addVenueAmenities(Long id, List<String> amenities) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getAmenities().addAll(amenities);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Remove venue amenities
   */
  public VenueResponseDTO removeVenueAmenities(Long id, List<String> amenities) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getAmenities().removeAll(amenities);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Add sports types
   */
  public VenueResponseDTO addSportsTypes(Long id, List<String> sportsTypes) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getSportsTypes().addAll(sportsTypes);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Remove sports types
   */
  public VenueResponseDTO removeSportsTypes(Long id, List<String> sportsTypes) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    venue.getSportsTypes().removeAll(sportsTypes);
    Venue updatedVenue = venueRepository.save(venue);

    return VenueMapper.toVenueResponseDTO(updatedVenue);
  }

  /**
   * Check venue availability
   */
  public Map<String, Object> checkVenueAvailability(Long id, String date, String startTime, String endTime) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    LocalDate localDate = LocalDate.parse(date);
    Map<String, Object> availability = new HashMap<>();

    // Check if venue is open on the given date
    boolean isOpen = venue.isOpen(localDate.getDayOfWeek(),
        startTime != null ? java.time.LocalTime.parse(startTime) : java.time.LocalTime.of(12, 0));

    availability.put("isOpen", isOpen);
    availability.put("date", date);
    availability.put("venueId", id);

    if (startTime != null && endTime != null) {
      // Check specific time slot availability
      availability.put("startTime", startTime);
      availability.put("endTime", endTime);
      availability.put("canBook", venue.canBookInAdvance(24)); // 24 hours in advance
    }

    return availability;
  }

  /**
   * Get venue pricing
   */
  public Map<String, Object> getVenuePricing(Long id) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    Map<String, Object> pricing = new HashMap<>();
    pricing.put("basePrice", venue.getBasePrice());
    pricing.put("dynamicPricingEnabled", venue.getDynamicPricingEnabled());
    pricing.put("peakHourMultiplier", venue.getPeakHourMultiplier());
    pricing.put("offPeakMultiplier", venue.getOffPeakMultiplier());
    pricing.put("weekendMultiplier", venue.getWeekendMultiplier());
    pricing.put("holidayMultiplier", venue.getHolidayMultiplier());
    pricing.put("peakHourStart", venue.getPeakHourStart());
    pricing.put("peakHourEnd", venue.getPeakHourEnd());

    return pricing;
  }

  /**
   * Get venue analytics
   */
  public Map<String, Object> getVenueAnalytics(Long id, String period) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    Map<String, Object> analytics = new HashMap<>();
    analytics.put("venueId", id);
    analytics.put("venueName", venue.getName());
    analytics.put("totalCourts", venue.getCourts().size());
    analytics.put("averageRating", venue.getAverageRating());
    analytics.put("totalReviews", venue.getTotalReviews());
    analytics.put("totalFavorites", venue.getFavorites().size());

    // Calculate booking statistics
    long totalBookings = venue.getCourts().stream()
        .mapToLong(court -> court.getBookedSlots().size())
        .sum();
    analytics.put("totalBookings", totalBookings);

    // Calculate occupancy rate
    double occupancyRate = venue.getCourts().stream()
        .mapToDouble(court -> court.getOccupancyRate())
        .average()
        .orElse(0.0);
    analytics.put("occupancyRate", occupancyRate);

    return analytics;
  }

  /**
   * Get venue reviews
   */
  public Map<String, Object> getVenueReviews(Long id, int page, int size, Integer minRating, String sortBy) {
    List<Review> allReviews = reviewRepository.findAll().stream()
        .filter(review -> review.getVenue().getVenueId().equals(id))
        .filter(review -> minRating == null || review.getRating() >= minRating)
        .collect(Collectors.toList());

    // Apply sorting
    if ("rating".equals(sortBy)) {
      allReviews.sort(Comparator.comparing(Review::getRating).reversed());
    } else if ("date".equals(sortBy)) {
      allReviews.sort(Comparator.comparing(Review::getReviewDate).reversed());
    }

    // Apply pagination
    int start = page * size;
    int end = Math.min(start + size, allReviews.size());

    List<Review> pageReviews = allReviews.subList(start, end);

    Map<String, Object> result = new HashMap<>();
    result.put("reviews", pageReviews);
    result.put("totalReviews", allReviews.size());
    result.put("page", page);
    result.put("size", size);
    result.put("totalPages", (int) Math.ceil((double) allReviews.size() / size));

    return result;
  }

  /**
   * Get venue average rating
   */
  public Map<String, Object> getVenueAverageRating(Long id) {
    Venue venue = venueRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    Map<String, Object> rating = new HashMap<>();
    rating.put("venueId", id);
    rating.put("venueName", venue.getName());
    rating.put("averageRating", venue.getAverageRating());
    rating.put("totalReviews", venue.getTotalReviews());

    return rating;
  }

  /**
   * Add venue to favorites
   */
  public void addToFavorites(Long id) {
    // This would require getting the current customer from security context
    // For now, we'll just create a placeholder
    System.out.println("Adding venue " + id + " to favorites");
  }

  /**
   * Remove venue from favorites
   */
  public void removeFromFavorites(Long id) {
    // This would require getting the current customer from security context
    // For now, we'll just create a placeholder
    System.out.println("Removing venue " + id + " from favorites");
  }

  /**
   * Get popular venues
   */
  public List<VenueResponseDTO> getPopularVenues(int limit, String location) {
    List<Venue> allVenues = venueRepository.findAll();

    return allVenues.stream()
        .filter(venue -> venue.getStatus() == Venue.VenueStatus.ACTIVE)
        .filter(venue -> location == null || venue.getLocation().toLowerCase().contains(location.toLowerCase()))
        .sorted(Comparator.comparingDouble(Venue::getAverageRating).reversed())
        .limit(limit)
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get trending venues
   */
  public List<VenueResponseDTO> getTrendingVenues(int limit, String period) {
    List<Venue> allVenues = venueRepository.findAll();

    return allVenues.stream()
        .filter(venue -> venue.getStatus() == Venue.VenueStatus.ACTIVE)
        .sorted(Comparator.comparingLong((Venue venue) -> venue.getCourts().stream()
            .mapToLong(court -> court.getBookedSlots().size())
            .sum()).reversed())
        .limit(limit)
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Calculate distance between two points using Haversine formula
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371; // Earth's radius in kilometers

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  // Legacy methods for backward compatibility
  public List<VenueResponseDTO> listVenues() {
    return venueRepository.findAll().stream()
        .map(VenueMapper::toVenueResponseDTO)
        .collect(Collectors.toList());
  }

  public List<VenueResponseDTO> listVenuesByOwner(Long ownerId) {
    return getVenuesByOwner(ownerId);
  }
}