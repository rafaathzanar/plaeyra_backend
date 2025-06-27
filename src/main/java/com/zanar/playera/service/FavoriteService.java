package com.zanar.playera.service;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.dto.TopFavoritedVenueDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Favorite;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.mapper.FavoriteMapper;
import com.zanar.playera.repo.FavoriteRepository;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteService {
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VenueRepository venueRepository;

    /**
     * Add a venue to customer's favorites
     */
    public FavoriteResponseDTO createFavorite(FavoriteRequestDTO dto) {
        // Validate customer exists
        Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + dto.getCustomerId()));
        
        // Validate venue exists
        Venue venue = venueRepository.findById(dto.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found with ID: " + dto.getVenueId()));
        
        // Check if already favorited
        if (favoriteRepository.existsByCustomerUserIdAndVenueVenueId(dto.getCustomerId(), dto.getVenueId())) {
            throw new RuntimeException("Venue is already in customer's favorites");
        }
        
        // Create new favorite
        Favorite favorite = new Favorite();
        favorite.setCustomer(customer);
        favorite.setVenue(venue);
        // Set addedDate to current time if not provided
        favorite.setAddedDate(dto.getAddedDate() != null ? dto.getAddedDate() : LocalDateTime.now());
        
        Favorite saved = favoriteRepository.save(favorite);
        return FavoriteMapper.toFavoriteResponseDTO(saved);
    }

    /**
     * Get favorite by ID
     */
    public FavoriteResponseDTO getFavoriteById(Long id) {
        Favorite favorite = favoriteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Favorite not found with ID: " + id));
        return FavoriteMapper.toFavoriteResponseDTO(favorite);
    }

    /**
     * Get all favorites (admin function)
     */
    public List<FavoriteResponseDTO> listFavorites() {
        return favoriteRepository.findAll().stream()
                .map(FavoriteMapper::toFavoriteResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get favorites by customer ID
     */
    public List<FavoriteResponseDTO> listFavoritesByCustomer(Long customerId) {
        // Validate customer exists
        if (!userRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }
        
        return favoriteRepository.findByCustomerUserId(customerId).stream()
                .map(FavoriteMapper::toFavoriteResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Remove favorite by ID
     */
    public void deleteFavorite(Long id) {
        if (!favoriteRepository.existsById(id)) {
            throw new RuntimeException("Favorite not found with ID: " + id);
        }
        favoriteRepository.deleteById(id);
    }

    /**
     * Remove specific venue from customer favorites
     */
    public void deleteFavoriteByCustomerAndVenue(Long customerId, Long venueId) {
        // Validate customer exists
        if (!userRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }
        
        // Validate venue exists
        if (!venueRepository.existsById(venueId)) {
            throw new RuntimeException("Venue not found with ID: " + venueId);
        }
        
        Favorite favorite = favoriteRepository.findByCustomerUserIdAndVenueVenueId(customerId, venueId)
                .orElseThrow(() -> new RuntimeException("Favorite not found for customer " + customerId + " and venue " + venueId));
        
        favoriteRepository.delete(favorite);
    }

    /**
     * Check if venue is in customer's favorites
     */
    public boolean isFavorite(Long customerId, Long venueId) {
        // Validate customer exists
        if (!userRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }
        
        // Validate venue exists
        if (!venueRepository.existsById(venueId)) {
            throw new RuntimeException("Venue not found with ID: " + venueId);
        }
        
        return favoriteRepository.existsByCustomerUserIdAndVenueVenueId(customerId, venueId);
    }

    /**
     * Get favorite count for a customer
     */
    public long getCustomerFavoriteCount(Long customerId) {
        // Validate customer exists
        if (!userRepository.existsById(customerId)) {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }
        
        return favoriteRepository.countByCustomerUserId(customerId);
    }

    /**
     * Get favorite count for a venue
     */
    public long getVenueFavoriteCount(Long venueId) {
        // Validate venue exists
        if (!venueRepository.existsById(venueId)) {
            throw new RuntimeException("Venue not found with ID: " + venueId);
        }
        
        return favoriteRepository.countByVenueVenueId(venueId);
    }

    /**
     * Get top favorited venues
     */
    public List<TopFavoritedVenueDTO> getTopFavoritedVenues() {
        List<Object[]> results = favoriteRepository.findTopFavoritedVenues();
        
        return results.stream()
                .map(result -> {
                    TopFavoritedVenueDTO dto = new TopFavoritedVenueDTO();
                    dto.setVenueId((Long) result[0]);
                    dto.setVenueName((String) result[1]);
                    dto.setFavoriteCount((Long) result[2]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get top favorited venues with limit
     */
    public List<TopFavoritedVenueDTO> getTopFavoritedVenuesWithLimit(int limit) {
        List<Object[]> results = favoriteRepository.findTopFavoritedVenuesWithLimit(limit);
        
        return results.stream()
                .limit(limit)
                .map(result -> {
                    TopFavoritedVenueDTO dto = new TopFavoritedVenueDTO();
                    dto.setVenueId((Long) result[0]);
                    dto.setVenueName((String) result[1]);
                    dto.setFavoriteCount((Long) result[2]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Toggle favorite status (add if not favorited, remove if favorited)
     */
    public FavoriteResponseDTO toggleFavorite(Long customerId, Long venueId) {
        // Check if already favorited
        if (isFavorite(customerId, venueId)) {
            // Remove from favorites
            deleteFavoriteByCustomerAndVenue(customerId, venueId);
            return null; // Indicates removal
        } else {
            // Add to favorites
            FavoriteRequestDTO dto = new FavoriteRequestDTO();
            dto.setCustomerId(customerId);
            dto.setVenueId(venueId);
            dto.setAddedDate(LocalDateTime.now());
            return createFavorite(dto);
        }
    }

    /**
     * Additional utility methods
     */
    public List<FavoriteResponseDTO> getFavoritesByVenue(Long venueId) {
        // Validate venue exists
        if (!venueRepository.existsById(venueId)) {
            throw new RuntimeException("Venue not found with ID: " + venueId);
        }
        
        return favoriteRepository.findByVenueVenueId(venueId).stream()
                .map(FavoriteMapper::toFavoriteResponseDTO)
                .collect(Collectors.toList());
    }
}