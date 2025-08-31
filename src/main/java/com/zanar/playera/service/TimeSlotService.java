package com.zanar.playera.service;

import com.zanar.playera.entity.*;
import com.zanar.playera.repo.BookingRepository;
import com.zanar.playera.repo.CourtRepository;
import com.zanar.playera.repo.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotService {

  private final CourtRepository courtRepository;
  private final SlotRepository slotRepository;
  private final BookingRepository bookingRepository;

  /**
   * Generate available time slots for a court on a specific date
   */
  public List<TimeSlotDTO> generateAvailableSlots(Long courtId, LocalDate date) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // Check if court is active on this date
    if (!isCourtAvailableOnDate(court, date)) {
      return new ArrayList<>();
    }

    // Generate all possible slots for the day
    List<TimeSlotDTO> allSlots = generateAllSlotsForDate(court, date);

    // Get all blocked/reserved slots for this date (not just booked)
    List<Slot> blockedSlots = slotRepository.findByCourt_CourtIdAndDateAndStatusIn(
        courtId, date, Arrays.asList(Slot.SlotStatus.BOOKED, Slot.SlotStatus.RESERVED, Slot.SlotStatus.MAINTENANCE));

    // Mark blocked slots as unavailable
    markBlockedSlotsAsUnavailable(allSlots, blockedSlots);

    // Filter only available slots
    return allSlots.stream()
        .filter(TimeSlotDTO::isAvailable)
        .collect(Collectors.toList());
  }

  /**
   * Generate all possible time slots for a court on a specific date
   */
  private List<TimeSlotDTO> generateAllSlotsForDate(Court court, LocalDate date) {
    List<TimeSlotDTO> slots = new ArrayList<>();

    if (court.getOpeningTime() == null || court.getClosingTime() == null ||
        court.getSlotDurationMinutes() == null) {
      return slots;
    }

    LocalTime currentTime = court.getOpeningTime();
    LocalTime closingTime = court.getClosingTime();
    int slotDuration = court.getSlotDurationMinutes();

    while (currentTime.isBefore(closingTime)) {
      LocalTime endTime = currentTime.plusMinutes(slotDuration);

      // Skip if end time exceeds closing time
      if (endTime.isAfter(closingTime)) {
        break;
      }

      // Check if this time slot is during break time
      if (!isDuringBreakTime(court, currentTime, endTime)) {
        TimeSlotDTO slot = TimeSlotDTO.builder()
            .startTime(currentTime)
            .endTime(endTime)
            .date(date)
            .courtId(court.getCourtId())
            .courtName(court.getCourtName())
            .venueName(court.getVenue().getName())
            .pricePerHour(court.getPricePerHour())
            .available(true)
            .status("AVAILABLE")
            .build();

        slots.add(slot);
      }

      currentTime = endTime;
    }

    return slots;
  }

  /**
   * Check if court is available on a specific date
   */
  private boolean isCourtAvailableOnDate(Court court, LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();

    // Check if court is active on weekends
    if ((dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) &&
        !Boolean.TRUE.equals(court.getIsActiveOnWeekends())) {
      return false;
    }

    // Check if court is active on holidays (you can implement holiday checking
    // logic)
    if (isHoliday(date) && !Boolean.TRUE.equals(court.getIsActiveOnHolidays())) {
      return false;
    }

    // Check if court status is active
    return Court.CourtStatus.ACTIVE.equals(court.getStatus());
  }

  /**
   * Check if a time slot is during break time
   */
  private boolean isDuringBreakTime(Court court, LocalTime startTime, LocalTime endTime) {
    if (!Boolean.TRUE.equals(court.getHasBreakTime()) ||
        court.getBreakStartTime() == null || court.getBreakEndTime() == null) {
      return false;
    }

    // Check if the slot overlaps with break time
    return !(endTime.isBefore(court.getBreakStartTime()) ||
        startTime.isAfter(court.getBreakEndTime()));
  }

  /**
   * Mark blocked slots as unavailable
   */
  private void markBlockedSlotsAsUnavailable(List<TimeSlotDTO> allSlots, List<Slot> blockedSlots) {
    for (Slot blockedSlot : blockedSlots) {
      for (TimeSlotDTO slot : allSlots) {
        if (slot.getStartTime().equals(blockedSlot.getStartTime()) &&
            slot.getEndTime().equals(blockedSlot.getEndTime())) {
          slot.setAvailable(false);
          slot.setBookingId(blockedSlot.getBooking() != null ? blockedSlot.getBooking().getBookingId() : null);

          // Set appropriate status based on slot type
          if (blockedSlot.getStatus() == Slot.SlotStatus.MAINTENANCE) {
            slot.setStatus("MAINTENANCE");
          } else if (blockedSlot.getStatus() == Slot.SlotStatus.RESERVED) {
            slot.setStatus("BLOCKED");
          } else if (blockedSlot.getStatus() == Slot.SlotStatus.BOOKED) {
            slot.setStatus("BOOKED");
          }
          break;
        }
      }
    }
  }

  /**
   * Check if a specific time slot is available for booking
   */
  public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    // Check if there are any overlapping bookings
    List<Slot> overlappingSlots = slotRepository.findOverlappingSlots(
        courtId, date, startTime, endTime);

    return overlappingSlots.isEmpty();
  }

  /**
   * Get slot availability for multiple dates (for calendar view)
   */
  public Map<LocalDate, List<TimeSlotDTO>> getSlotsForDateRange(Long courtId, LocalDate startDate, LocalDate endDate) {
    Map<LocalDate, List<TimeSlotDTO>> slotsByDate = new LinkedHashMap<>();

    LocalDate currentDate = startDate;
    while (!currentDate.isAfter(endDate)) {
      List<TimeSlotDTO> slots = generateAvailableSlots(courtId, currentDate);
      slotsByDate.put(currentDate, slots);
      currentDate = currentDate.plusDays(1);
    }

    return slotsByDate;
  }

  /**
   * Block a time slot (for maintenance, reservations, etc.)
   */
  public void blockTimeSlot(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime, String reason,
      boolean isMaintenance) {
    // Check if slot is already blocked
    List<Slot> existingSlots = slotRepository.findByCourt_CourtIdAndDateAndStartTimeAndEndTimeAndStatusIn(
        courtId, date, startTime, endTime,
        Arrays.asList(Slot.SlotStatus.RESERVED, Slot.SlotStatus.MAINTENANCE));

    if (!existingSlots.isEmpty()) {
      log.warn("Slot already blocked for court {} on {} from {} to {}", courtId, date, startTime, endTime);
      return;
    }

    Slot blockedSlot = new Slot();
    blockedSlot.setCourt(courtRepository.findById(courtId).orElse(null));
    blockedSlot.setDate(date);
    blockedSlot.setStartTime(startTime);
    blockedSlot.setEndTime(endTime);

    // Set status based on whether it's maintenance or general blocking
    if (isMaintenance) {
      blockedSlot.setStatus(Slot.SlotStatus.MAINTENANCE);
    } else {
      blockedSlot.setStatus(Slot.SlotStatus.RESERVED);
    }

    slotRepository.save(blockedSlot);
    log.info("Blocked time slot for court {} on {} from {} to {}: {} (Status: {})",
        courtId, date, startTime, endTime, reason, blockedSlot.getStatus());
  }

  /**
   * Block recurring time slots
   */
  public void blockRecurringTimeSlots(Long courtId, LocalDate startDate, LocalDate endDate,
      LocalTime startTime, LocalTime endTime, String reason,
      boolean isMaintenance, List<Integer> recurringDays) {
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
      int dayValue = dayOfWeek.getValue() - 1; // Convert to 0-based index (Monday = 0)

      if (recurringDays.contains(dayValue)) {
        blockTimeSlot(courtId, currentDate, startTime, endTime, reason, isMaintenance);
      }

      currentDate = currentDate.plusDays(1);
    }
  }

  /**
   * Unblock a time slot
   */
  public void unblockTimeSlot(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    // Find slots with any blocked status
    List<Slot> blockedSlots = slotRepository.findByCourt_CourtIdAndDateAndStartTimeAndEndTimeAndStatusIn(
        courtId, date, startTime, endTime,
        Arrays.asList(Slot.SlotStatus.RESERVED, Slot.SlotStatus.MAINTENANCE));

    for (Slot blockedSlot : blockedSlots) {
      slotRepository.delete(blockedSlot);
      log.info("Unblocked time slot for court {} on {} from {} to {} (Status: {})",
          courtId, date, startTime, endTime, blockedSlot.getStatus());
    }
  }

  /**
   * Get all time slots for a court on a specific date (including blocked ones)
   */
  public List<TimeSlotDTO> getAllTimeSlotsForDate(Long courtId, LocalDate date) {
    Court court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // Generate all possible slots for the day
    List<TimeSlotDTO> allSlots = generateAllSlotsForDate(court, date);

    // Get all blocked/reserved slots for this date
    List<Slot> blockedSlots = slotRepository.findByCourt_CourtIdAndDateAndStatusIn(
        courtId, date, Arrays.asList(Slot.SlotStatus.BOOKED, Slot.SlotStatus.RESERVED, Slot.SlotStatus.MAINTENANCE));

    // Mark blocked slots as unavailable
    markBlockedSlotsAsUnavailable(allSlots, blockedSlots);

    return allSlots;
  }

  /**
   * Get peak hours for a court (for dynamic pricing)
   */
  public List<LocalTime> getPeakHours(Long courtId) {
    // This could be configurable per court or venue
    // For now, return standard peak hours (6 PM - 10 PM)
    return Arrays.asList(
        LocalTime.of(18, 0), // 6 PM
        LocalTime.of(19, 0), // 7 PM
        LocalTime.of(20, 0), // 8 PM
        LocalTime.of(21, 0) // 9 PM
    );
  }

  /**
   * Check if a date is a holiday (you can implement your own holiday logic)
   */
  private boolean isHoliday(LocalDate date) {
    // Simple holiday checking - you can enhance this with a holiday API or database
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    // Example holidays (Sri Lanka)
    return (month == 1 && day == 1) || // New Year
        (month == 4 && day == 13) || // Sinhala & Tamil New Year
        (month == 5 && day == 1) || // May Day
        (month == 12 && day == 25); // Christmas
  }

  /**
   * DTO for time slot information
   */
  public static class TimeSlotDTO {
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate date;
    private Long courtId;
    private String courtName;
    private String venueName;
    private java.math.BigDecimal pricePerHour;
    private boolean available;
    private Long bookingId;
    private String status;

    // Builder pattern
    public static TimeSlotDTOBuilder builder() {
      return new TimeSlotDTOBuilder();
    }

    // Getters and setters
    public LocalTime getStartTime() {
      return startTime;
    }

    public void setStartTime(LocalTime startTime) {
      this.startTime = startTime;
    }

    public LocalTime getEndTime() {
      return endTime;
    }

    public void setEndTime(LocalTime endTime) {
      this.endTime = endTime;
    }

    public LocalDate getDate() {
      return date;
    }

    public void setDate(LocalDate date) {
      this.date = date;
    }

    public Long getCourtId() {
      return courtId;
    }

    public void setCourtId(Long courtId) {
      this.courtId = courtId;
    }

    public String getCourtName() {
      return courtName;
    }

    public void setCourtName(String courtName) {
      this.courtName = courtName;
    }

    public String getVenueName() {
      return venueName;
    }

    public void setVenueName(String venueName) {
      this.venueName = venueName;
    }

    public java.math.BigDecimal getPricePerHour() {
      return pricePerHour;
    }

    public void setPricePerHour(java.math.BigDecimal pricePerHour) {
      this.pricePerHour = pricePerHour;
    }

    public boolean isAvailable() {
      return available;
    }

    public void setAvailable(boolean available) {
      this.available = available;
    }

    public Long getBookingId() {
      return bookingId;
    }

    public void setBookingId(Long bookingId) {
      this.bookingId = bookingId;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public static class TimeSlotDTOBuilder {
      private TimeSlotDTO timeSlotDTO;

      public TimeSlotDTOBuilder() {
        timeSlotDTO = new TimeSlotDTO();
      }

      public TimeSlotDTOBuilder startTime(LocalTime startTime) {
        timeSlotDTO.startTime = startTime;
        return this;
      }

      public TimeSlotDTOBuilder endTime(LocalTime endTime) {
        timeSlotDTO.endTime = endTime;
        return this;
      }

      public TimeSlotDTOBuilder date(LocalDate date) {
        timeSlotDTO.date = date;
        return this;
      }

      public TimeSlotDTOBuilder courtId(Long courtId) {
        timeSlotDTO.courtId = courtId;
        return this;
      }

      public TimeSlotDTOBuilder courtName(String courtName) {
        timeSlotDTO.courtName = courtName;
        return this;
      }

      public TimeSlotDTOBuilder venueName(String venueName) {
        timeSlotDTO.venueName = venueName;
        return this;
      }

      public TimeSlotDTOBuilder pricePerHour(java.math.BigDecimal pricePerHour) {
        timeSlotDTO.pricePerHour = pricePerHour;
        return this;
      }

      public TimeSlotDTOBuilder available(boolean available) {
        timeSlotDTO.available = available;
        return this;
      }

      public TimeSlotDTOBuilder status(String status) {
        timeSlotDTO.status = status;
        return this;
      }

      public TimeSlotDTO build() {
        return timeSlotDTO;
      }
    }
  }
}
