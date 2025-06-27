package com.zanar.playera.repo;

import com.zanar.playera.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    /**
     * Find all favorites by customer ID
     */
    List<Favorite> findByCustomerUserId(Long customerId);
    
    /**
     * Find favorites by venue ID
     */
    List<Favorite> findByVenueVenueId(Long venueId);
    
    /**
     * Find favorite by customer ID and venue ID
     */
    Optional<Favorite> findByCustomerUserIdAndVenueVenueId(Long customerId, Long venueId);
    
    /**
     * Check if a venue is favorited by a customer
     */
    boolean existsByCustomerUserIdAndVenueVenueId(Long customerId, Long venueId);
    
    /**
     * Delete favorite by customer ID and venue ID
     */
    void deleteByCustomerUserIdAndVenueVenueId(Long customerId, Long venueId);
    
    /**
     * Count favorites by customer ID
     */
    long countByCustomerUserId(Long customerId);
    
    /**
     * Count favorites by venue ID
     */
    long countByVenueVenueId(Long venueId);
    
    /**
     * Find favorites with venue details using custom query for better performance
     */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.venue v JOIN FETCH f.customer c WHERE c.userId = :customerId")
    List<Favorite> findFavoritesWithVenueDetailsByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * Find top favorited venues
     */
    @Query("SELECT f.venue.venueId as venueId, f.venue.name as venueName, COUNT(f) as favoriteCount " +
           "FROM Favorite f " +
           "GROUP BY f.venue.venueId, f.venue.name " +
           "ORDER BY favoriteCount DESC")
    List<Object[]> findTopFavoritedVenues();
    
    /**
     * Find top favorited venues with limit
     */
    @Query("SELECT f.venue.venueId as venueId, f.venue.name as venueName, COUNT(f) as favoriteCount " +
           "FROM Favorite f " +
           "GROUP BY f.venue.venueId, f.venue.name " +
           "ORDER BY favoriteCount DESC")
    List<Object[]> findTopFavoritedVenuesWithLimit(@Param("limit") int limit);
}