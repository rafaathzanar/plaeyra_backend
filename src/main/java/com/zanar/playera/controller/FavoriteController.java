package com.zanar.playera.controller;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.dto.TopFavoritedVenueDTO;
import com.zanar.playera.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "Favorite Management", description = "APIs for managing user favorites")
@Validated
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping
    @Operation(summary = "Add a venue to favorites", description = "Add a venue to a customer's favorites list")
    public ResponseEntity<FavoriteResponseDTO> addToFavorites(@RequestBody FavoriteRequestDTO dto) {
        try {
            FavoriteResponseDTO response = favoriteService.createFavorite(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all favorites", description = "Retrieve all favorites (admin only)")
    public ResponseEntity<List<FavoriteResponseDTO>> getAllFavorites() {
        List<FavoriteResponseDTO> favorites = favoriteService.listFavorites();
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get favorite by ID", description = "Retrieve a specific favorite by its ID")
    public ResponseEntity<FavoriteResponseDTO> getFavoriteById(@PathVariable Long id) {
        try {
            FavoriteResponseDTO response = favoriteService.getFavoriteById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer favorites", description = "Retrieve all favorites for a specific customer")
    public ResponseEntity<List<FavoriteResponseDTO>> getFavoritesByCustomer(@PathVariable Long customerId) {
        List<FavoriteResponseDTO> favorites = favoriteService.listFavoritesByCustomer(customerId);
        return ResponseEntity.ok(favorites);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove favorite", description = "Remove a venue from favorites")
    public ResponseEntity<Void> removeFromFavorites(@PathVariable Long id) {
        try {
            favoriteService.deleteFavorite(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/customer/{customerId}/venue/{venueId}")
    @Operation(summary = "Remove specific venue from customer favorites", description = "Remove a specific venue from a customer's favorites")
    public ResponseEntity<Void> removeFavoriteByCustomerAndVenue(
            @PathVariable Long customerId, 
            @PathVariable Long venueId) {
        try {
            favoriteService.deleteFavoriteByCustomerAndVenue(customerId, venueId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/check/customer/{customerId}/venue/{venueId}")
    @Operation(summary = "Check if venue is in favorites", description = "Check if a specific venue is in customer's favorites")
    public ResponseEntity<Boolean> checkIfFavorite(
            @PathVariable Long customerId, 
            @PathVariable Long venueId) {
        boolean isFavorite = favoriteService.isFavorite(customerId, venueId);
        return ResponseEntity.ok(isFavorite);
    }

    @GetMapping("/customer/{customerId}/count")
    @Operation(summary = "Get customer favorite count", description = "Get the total number of favorites for a customer")
    public ResponseEntity<Long> getCustomerFavoriteCount(@NotNull @PathVariable Long customerId) {
        try {
            long count = favoriteService.getCustomerFavoriteCount(customerId);
            return ResponseEntity.ok(count);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/venue/{venueId}/count")
    @Operation(summary = "Get venue favorite count", description = "Get the total number of times a venue has been favorited")
    public ResponseEntity<Long> getVenueFavoriteCount(@NotNull @PathVariable Long venueId) {
        try {
            long count = favoriteService.getVenueFavoriteCount(venueId);
            return ResponseEntity.ok(count);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/top-venues")
    @Operation(summary = "Get top favorited venues", description = "Get venues ordered by number of favorites")
    public ResponseEntity<List<TopFavoritedVenueDTO>> getTopFavoritedVenues() {
        try {
            List<TopFavoritedVenueDTO> topVenues = favoriteService.getTopFavoritedVenues();
            return ResponseEntity.ok(topVenues);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/toggle/customer/{customerId}/venue/{venueId}")
    @Operation(summary = "Toggle favorite status", description = "Add venue to favorites if not favorited, remove if already favorited")
    public ResponseEntity<FavoriteResponseDTO> toggleFavorite(
            @PathVariable Long customerId,
            @PathVariable Long venueId) {
        try {
            FavoriteResponseDTO result = favoriteService.toggleFavorite(customerId, venueId);
            if (result != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 