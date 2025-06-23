package com.zanar.playera.repo;

import com.zanar.playera.entity.BookingCourt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingCourtRepository extends JpaRepository<BookingCourt, Long> {
}