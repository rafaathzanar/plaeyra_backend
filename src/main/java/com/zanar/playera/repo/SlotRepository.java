package com.zanar.playera.repo;

import com.zanar.playera.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByCourtIdAndStatus(Long courtId, Slot.SlotStatus status);
}
