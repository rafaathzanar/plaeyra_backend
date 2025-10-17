package com.zanar.playera.repo;

import com.zanar.playera.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.customer " +
            "LEFT JOIN FETCH b.bookingCourts bc " +
            "LEFT JOIN FETCH bc.court c " +
            "LEFT JOIN FETCH c.venue " +
            "WHERE b.customer.userId = :customerId")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.customer " +
            "LEFT JOIN FETCH b.bookingCourts bc " +
            "LEFT JOIN FETCH bc.court c " +
            "LEFT JOIN FETCH c.venue " +
            "WHERE c.venue.venueId = :venueId")
    List<Booking> findByVenueIdWithDetails(@Param("venueId") Long venueId);

    // Separate queries to avoid MultipleBagFetchException
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingEquipments be " +
            "LEFT JOIN FETCH be.equipment " +
            "WHERE b.bookingId = :bookingId")
    Booking findByIdWithEquipment(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingTimeSlots " +
            "WHERE b.bookingId = :bookingId")
    Booking findByIdWithTimeSlots(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.bookingCourts bc " +
            "LEFT JOIN FETCH bc.court " +
            "WHERE b.bookingId = :bookingId")
    Booking findByIdWithCourts(@Param("bookingId") Long bookingId);
}
