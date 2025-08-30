package com.zanar.playera.service;

import com.zanar.playera.dto.CourtRequestDTO;
import com.zanar.playera.dto.CourtResponseDTO;
import com.zanar.playera.entity.Court;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.Equipment;
import com.zanar.playera.entity.Booking;
import com.zanar.playera.mapper.CourtMapper;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.VenueRepository;
import com.zanar.playera.repo.EquipmentRepository;
import com.zanar.playera.repo.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourtService {

  @Autowired
  private CourtRepository courtRepository;

  @Autowired
  private VenueRepository venueRepository;

  @Autowired
  private EquipmentRepository equipmentRepository;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private CourtMapper courtMapper;

  /**
   * Create a new court
   */
  public CourtResponseDTO createCourt(CourtRequestDTO courtRequestDTO) {
    Venue venue = venueRepository.findById(courtRequestDTO.getVenueId())
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    Court court = CourtMapper.toCourtEntity(courtRequestDTO, venue);
    court.setStatus(Court.CourtStatus.ACTIVE);

    Court savedCourt = courtRepository.save(court);
    venue.addCourt(savedCourt);
    venueRepository.save(venue);

    return CourtMapper.toCourtResponseDTO(savedCourt);
  }

  /**
   * Get court by ID
   */
  public CourtResponseDTO getCourtById(Long id) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));
    return courtMapper.toCourtResponseDTO(court);
  }

  /**
   * Get all courts with filtering and pagination
   */
  public Page<CourtResponseDTO> getAllCourts(Long venueId, String sportType,
      Boolean isIndoor, Boolean isLighted, Boolean isAirConditioned,
      Double minPrice, Double maxPrice, String status, Pageable pageable) {

    List<Court> allCourts = courtRepository.findAll();

    // Apply filters
    List<Court> filteredCourts = allCourts.stream()
        .filter(court -> venueId == null || court.getVenue().getVenueId().equals(venueId))
        .filter(court -> sportType == null || court.getType().name().equals(sportType))

        .filter(court -> isIndoor == null || court.getIsIndoor() == isIndoor)
        .filter(court -> isLighted == null || court.getIsLighted() == isLighted)
        .filter(court -> isAirConditioned == null || court.getIsAirConditioned() == isAirConditioned)
        .filter(court -> minPrice == null || court.getPricePerHour().compareTo(BigDecimal.valueOf(minPrice)) >= 0)
        .filter(court -> maxPrice == null || court.getPricePerHour().compareTo(BigDecimal.valueOf(maxPrice)) <= 0)
        .filter(court -> status == null || court.getStatus().name().equals(status))
        .filter(court -> court.getStatus() != Court.CourtStatus.DELETED)
        .collect(Collectors.toList());

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), filteredCourts.size());

    if (start > filteredCourts.size()) {
      return new PageImpl<>(new ArrayList<>(), pageable, filteredCourts.size());
    }

    List<Court> pageContent = filteredCourts.subList(start, end);
    List<CourtResponseDTO> courtDTOs = pageContent.stream()
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());

    return new PageImpl<>(courtDTOs, pageable, filteredCourts.size());
  }

  /**
   * Get courts by venue
   */
  public List<CourtResponseDTO> getCourtsByVenue(Long venueId) {
    List<Court> courts = courtRepository.findAll().stream()
        .filter(court -> court.getVenue().getVenueId().equals(venueId))
        .filter(court -> court.getStatus() != Court.CourtStatus.DELETED)
        .collect(Collectors.toList());

    return courts.stream()
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Search courts by query
   */
  public List<CourtResponseDTO> searchCourts(String query, Long venueId, String sportType) {
    List<Court> allCourts = courtRepository.findAll();

    return allCourts.stream()
        .filter(court -> court.getStatus() != Court.CourtStatus.DELETED)
        .filter(court -> venueId == null || court.getVenue().getVenueId().equals(venueId))
        .filter(court -> sportType == null || court.getType().name().equals(sportType))
        .filter(court -> query == null ||
            court.getCourtName().toLowerCase().contains(query.toLowerCase()) ||
            court.getDescription().toLowerCase().contains(query.toLowerCase()) ||
            court.getType().name().toLowerCase().contains(query.toLowerCase()))
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Find available courts
   */
  public List<CourtResponseDTO> getAvailableCourts(LocalDate date, LocalTime startTime,
      LocalTime endTime, Long venueId, String sportType) {
    List<Court> allCourts = courtRepository.findAll();

    return allCourts.stream()
        .filter(court -> court.getStatus() == Court.CourtStatus.ACTIVE)
        .filter(court -> venueId == null || court.getVenue().getVenueId().equals(venueId))
        .filter(court -> sportType == null || court.getType().name().equals(sportType))
        .filter(court -> court.isAvailable(date.getDayOfWeek(), startTime))
        .filter(court -> !court.isUnderMaintenance(startTime))
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Update court
   */
  public CourtResponseDTO updateCourt(Long id, CourtRequestDTO courtRequestDTO) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // Update court fields from DTO
    if (courtRequestDTO.getCourtName() != null)
      court.setCourtName(courtRequestDTO.getCourtName());
    if (courtRequestDTO.getType() != null)
      court.setType(Court.CourtType.valueOf(courtRequestDTO.getType().toUpperCase()));
    if (courtRequestDTO.getCapacity() > 0)
      court.setCapacity(courtRequestDTO.getCapacity());
    if (courtRequestDTO.getPricePerHour() > 0)
      court.setPricePerHour(BigDecimal.valueOf(courtRequestDTO.getPricePerHour()));

    Court updatedCourt = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Delete court
   */
  public void deleteCourt(Long id) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.setStatus(Court.CourtStatus.DELETED);
    courtRepository.save(court);
  }

  /**
   * Update court status
   */
  public CourtResponseDTO updateCourtStatus(Long id, String status) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.setStatus(Court.CourtStatus.valueOf(status.toUpperCase()));
    Court updatedCourt = courtRepository.save(court);

    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Set court maintenance mode
   */
  public CourtResponseDTO setMaintenanceMode(Long id, LocalTime startTime, LocalTime endTime, String notes) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.setMaintenanceMode(true);
    court.setMaintenanceStartTime(startTime);
    court.setMaintenanceEndTime(endTime);
    court.setStatus(Court.CourtStatus.MAINTENANCE);

    Court updatedCourt = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Remove court maintenance mode
   */
  public CourtResponseDTO removeMaintenanceMode(Long id) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.setMaintenanceMode(false);
    court.setMaintenanceStartTime(null);
    court.setMaintenanceEndTime(null);
    court.setStatus(Court.CourtStatus.ACTIVE);

    Court updatedCourt = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Check court availability
   */
  public Map<String, Object> checkCourtAvailability(Long id, LocalDate date, LocalTime startTime, LocalTime endTime) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> availability = new HashMap<>();
    availability.put("courtId", id);
    availability.put("courtName", court.getCourtName());
    availability.put("date", date);

    if (startTime != null && endTime != null) {
      boolean isAvailable = court.isAvailable(date.getDayOfWeek(), startTime);
      boolean isUnderMaintenance = court.isUnderMaintenance(startTime);
      boolean canBookDuration = court.canBookDuration(
          endTime.getHour() - startTime.getHour());

      availability.put("isAvailable", isAvailable);
      availability.put("isUnderMaintenance", isUnderMaintenance);
      availability.put("canBookDuration", canBookDuration);
      availability.put("startTime", startTime);
      availability.put("endTime", endTime);
    }

    return availability;
  }

  /**
   * Get court pricing
   */
  public Map<String, Object> getCourtPricing(Long id) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> pricing = new HashMap<>();
    pricing.put("courtId", id);
    pricing.put("basePrice", court.getPricePerHour());
    pricing.put("dynamicPricingEnabled", court.getDynamicPricingEnabled());
    pricing.put("peakHourMultiplier", court.getPeakHourMultiplier());
    pricing.put("offPeakMultiplier", court.getOffPeakMultiplier());
    pricing.put("weekendMultiplier", court.getWeekendMultiplier());
    // holidayMultiplier method not available in Court entity
    pricing.put("peakHourStart", court.getPeakHourStart());
    pricing.put("peakHourEnd", court.getPeakHourEnd());

    return pricing;
  }

  /**
   * Get court time slots
   */
  public Map<String, Object> getCourtSlots(Long id, LocalDate date) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> slots = new HashMap<>();
    slots.put("courtId", id);
    slots.put("date", date);
    slots.put("availableSlots", court.getAvailableSlots());
    slots.put("bookedSlots", court.getBookedSlots());

    return slots;
  }

  /**
   * Get court equipment
   */
  public List<Map<String, Object>> getCourtEquipment(Long id) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    return court.getEquipmentList().stream()
        .map(equipment -> {
          Map<String, Object> eq = new HashMap<>();
          eq.put("equipmentId", equipment.getEquipmentId());
          eq.put("name", equipment.getName());
          eq.put("description", equipment.getDescription());
          eq.put("isAvailable", equipment.isAvailable());
          return eq;
        })
        .collect(Collectors.toList());
  }

  /**
   * Add equipment to court
   */
  public CourtResponseDTO addEquipment(Long id, List<Long> equipmentIds) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    List<Equipment> equipmentList = equipmentRepository.findAllById(equipmentIds);
    court.getEquipmentList().addAll(equipmentList);

    Court updatedCourt = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Remove equipment from court
   */
  public CourtResponseDTO removeEquipment(Long id, List<Long> equipmentIds) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    court.getEquipmentList().removeIf(equipment -> equipmentIds.contains(equipment.getEquipmentId()));

    Court updatedCourt = courtRepository.save(court);
    return CourtMapper.toCourtResponseDTO(updatedCourt);
  }

  /**
   * Get court analytics
   */
  public Map<String, Object> getCourtAnalytics(Long id, String period) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> analytics = new HashMap<>();
    analytics.put("courtId", id);
    analytics.put("courtName", court.getCourtName());
    analytics.put("totalBookings", court.getBookedSlots().size());
    analytics.put("occupancyRate", court.getOccupancyRate());
    analytics.put("averageRating", 0.0); // Would need to calculate from reviews
    analytics.put("totalEquipment", court.getEquipmentList().size());

    return analytics;
  }

  /**
   * Get court occupancy rate
   */
  public Map<String, Object> getCourtOccupancy(Long id, String period) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    Map<String, Object> occupancy = new HashMap<>();
    occupancy.put("courtId", id);
    occupancy.put("courtName", court.getCourtName());
    occupancy.put("occupancyRate", court.getOccupancyRate());
    occupancy.put("totalSlots", court.getSlots().size());
    occupancy.put("bookedSlots", court.getBookedSlots().size());
    occupancy.put("availableSlots", court.getAvailableSlots().size());

    return occupancy;
  }

  /**
   * Get court bookings
   */
  public Map<String, Object> getCourtBookings(Long id, LocalDate date, int page, int size) {
    Court court = courtRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    List<Booking> allBookings = bookingRepository.findAll().stream()
        .filter(booking -> booking.getBookingCourts().stream()
            .anyMatch(bc -> bc.getCourt().getCourtId().equals(id)))
        .filter(booking -> date == null ||
            booking.getBookingDate().toLocalDate().equals(date))
        .collect(Collectors.toList());

    // Apply pagination
    int start = page * size;
    int end = Math.min(start + size, allBookings.size());

    List<Booking> pageBookings = allBookings.subList(start, end);

    Map<String, Object> result = new HashMap<>();
    result.put("bookings", pageBookings);
    result.put("totalBookings", allBookings.size());
    result.put("page", page);
    result.put("size", size);
    result.put("totalPages", (int) Math.ceil((double) allBookings.size() / size));

    return result;
  }

  /**
   * Get popular courts
   */
  public List<CourtResponseDTO> getPopularCourts(int limit, Long venueId) {
    List<Court> allCourts = courtRepository.findAll();

    return allCourts.stream()
        .filter(court -> court.getStatus() != Court.CourtStatus.DELETED)
        .filter(court -> venueId == null || court.getVenue().getVenueId().equals(venueId))
        .sorted(Comparator.comparingLong((Court court) -> court.getBookedSlots().size()).reversed())
        .limit(limit)
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  /**
   * Get court recommendations
   */
  public List<CourtResponseDTO> getCourtRecommendations(Long customerId, String sportType,
      String location, int limit) {
    List<Court> allCourts = courtRepository.findAll();

    return allCourts.stream()
        .filter(court -> court.getStatus() == Court.CourtStatus.ACTIVE)
        .filter(court -> sportType == null || court.getType().equals(sportType))
        .filter(court -> location == null ||
            court.getVenue().getLocation().toLowerCase().contains(location.toLowerCase()))
        .sorted(Comparator.comparingDouble(Court::getOccupancyRate))
        .limit(limit)
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  // Legacy methods for backward compatibility
  public List<CourtResponseDTO> listCourts() {
    return courtRepository.findAll().stream()
        .map(CourtMapper::toCourtResponseDTO)
        .collect(Collectors.toList());
  }

  public List<CourtResponseDTO> listCourtsByVenue(Long venueId) {
    return getCourtsByVenue(venueId);
  }

  public CourtResponseDTO createCourt(CourtRequestDTO dto, Long ownerId) {
    // For backward compatibility, we'll use the venue owner to find the venue
    List<Venue> venues = venueRepository.findAll().stream()
        .filter(venue -> venue.getVenueOwner().getUserId().equals(ownerId))
        .collect(Collectors.toList());

    if (venues.isEmpty()) {
      throw new RuntimeException("No venues found for owner");
    }

    // Use the first venue found
    CourtRequestDTO modifiedDto = new CourtRequestDTO();
    modifiedDto.setVenueId(venues.get(0).getVenueId());
    // Copy other fields from original DTO
    // This is a simplified approach for backward compatibility

    return createCourt(modifiedDto);
  }

  public CourtResponseDTO updateCourt(Long id, CourtRequestDTO dto, Long ownerId) {
    // For backward compatibility, we'll just call the new update method
    return updateCourt(id, dto);
  }
}