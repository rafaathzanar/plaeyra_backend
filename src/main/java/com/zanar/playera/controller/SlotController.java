package com.zanar.playera.controller;

import com.zanar.playera.entity.Slot;
import com.zanar.playera.repo.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {
    @Autowired
    private SlotRepository slotRepository;

    @GetMapping("/available/{courtId}")
    public List<Slot> getAvailableSlots(@PathVariable Long courtId) {
        return slotRepository.findByCourtIdAndStatus(courtId, Slot.SlotStatus.AVAILABLE);
    }
}
