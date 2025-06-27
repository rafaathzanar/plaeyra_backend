package com.zanar.playera.service;

import com.zanar.playera.entity.Slot;
import com.zanar.playera.repo.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class SlotService {
    
    @Autowired
    private SlotRepository slotRepository;
    
    public List<Slot> getAvailableSlotsByCourt(Long courtId) {
        return slotRepository.findByCourtIdAndStatus(courtId, Slot.SlotStatus.AVAILABLE);
    }
    
    public List<Slot> getAvailableSlotsByCourtAndDate(Long courtId, LocalDate date) {
        return slotRepository.findByCourtIdAndDateAndStatus(courtId, date, Slot.SlotStatus.AVAILABLE);
    }
    
    public List<Slot> getSlotsByCourtAndStatus(Long courtId, Slot.SlotStatus status) {
        return slotRepository.findByCourtIdAndStatus(courtId, status);
    }
    
    public List<Slot> getCourtCalendar(Long courtId, LocalDate startDate, LocalDate endDate) {
        return slotRepository.findByCourtIdAndDateBetween(courtId, startDate, endDate);
    }
    
    public List<Slot> getAvailableSlotsByDateTimeRange(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return slotRepository.findAvailableSlotsByCourtAndDateTimeRange(courtId, date, Slot.SlotStatus.AVAILABLE, startTime, endTime);
    }
    
    public List<Slot> getConflictingSlots(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return slotRepository.findConflictingSlots(courtId, date, startTime, endTime);
    }
    
    public List<Slot> getSlotsByBooking(Long bookingId) {
        return slotRepository.findByBookingId(bookingId);
    }
    
    public long getAvailableSlotCount(Long courtId, LocalDate date) {
        return slotRepository.countByCourtIdAndDateAndStatus(courtId, date, Slot.SlotStatus.AVAILABLE);
    }
    
    public void cleanupExpiredSlots() {
        List<Slot> expiredSlots = slotRepository.findExpiredBookedSlots(LocalDate.now());
        for (Slot slot : expiredSlots) {
            slot.release();
            slotRepository.save(slot);
        }
    }
    
    public Slot createSlot(Slot slot) {
        return slotRepository.save(slot);
    }
    
    public Slot updateSlotStatus(Long slotId, Slot.SlotStatus status) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slot.setStatus(status);
        return slotRepository.save(slot);
    }
    
    public void deleteSlot(Long slotId) {
        slotRepository.deleteById(slotId);
    }
} 