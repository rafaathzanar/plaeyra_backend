package com.zanar.playera.repo;

import com.zanar.playera.entity.BookingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {
}