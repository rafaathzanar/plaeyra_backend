package com.zanar.playera;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Favorite;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.repo.FavoriteRepository;
import com.zanar.playera.service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FavoriteSystemTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    private Customer testCustomer;
    private Venue testVenue;

    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("test@example.com");
        testCustomer.setPassword("password");
        testCustomer.setPhone("1234567890");

        // Create test venue
        testVenue = new Venue();
        testVenue.setName("Test Venue");
        testVenue.setLocation("Test Location");
        testVenue.setDescription("Test Description");
        testVenue.setContactNo("0987654321");
    }

    @Test
    void testCreateFavorite() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(1L);
        request.setVenueId(1L);

        // When
        FavoriteResponseDTO response = favoriteService.createFavorite(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertEquals(1L, response.getVenueId());
        assertNotNull(response.getAddedDate());
    }

    @Test
    void testDuplicateFavorite() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(1L);
        request.setVenueId(1L);

        // When - Add first favorite
        favoriteService.createFavorite(request);

        // Then - Adding duplicate should throw exception
        assertThrows(RuntimeException.class, () -> {
            favoriteService.createFavorite(request);
        });
    }

    @Test
    void testIsFavorite() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(1L);
        request.setVenueId(1L);

        // When - Before adding
        boolean isFavoriteBefore = favoriteService.isFavorite(1L, 1L);

        // Then
        assertFalse(isFavoriteBefore);

        // When - After adding
        favoriteService.createFavorite(request);
        boolean isFavoriteAfter = favoriteService.isFavorite(1L, 1L);

        // Then
        assertTrue(isFavoriteAfter);
    }

    @Test
    void testToggleFavorite() {
        // Given
        Long customerId = 1L;
        Long venueId = 1L;

        // When - Toggle to add
        FavoriteResponseDTO added = favoriteService.toggleFavorite(customerId, venueId);

        // Then
        assertNotNull(added);
        assertTrue(favoriteService.isFavorite(customerId, venueId));

        // When - Toggle to remove
        FavoriteResponseDTO removed = favoriteService.toggleFavorite(customerId, venueId);

        // Then
        assertNull(removed);
        assertFalse(favoriteService.isFavorite(customerId, venueId));
    }

    @Test
    void testGetCustomerFavorites() {
        // Given
        FavoriteRequestDTO request1 = new FavoriteRequestDTO();
        request1.setCustomerId(1L);
        request1.setVenueId(1L);

        FavoriteRequestDTO request2 = new FavoriteRequestDTO();
        request2.setCustomerId(1L);
        request2.setVenueId(2L);

        // When
        favoriteService.createFavorite(request1);
        favoriteService.createFavorite(request2);

        // Then
        var favorites = favoriteService.listFavoritesByCustomer(1L);
        assertEquals(2, favorites.size());
    }

    @Test
    void testDeleteFavorite() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(1L);
        request.setVenueId(1L);
        FavoriteResponseDTO created = favoriteService.createFavorite(request);

        // When
        favoriteService.deleteFavorite(created.getFavoriteId());

        // Then
        assertFalse(favoriteService.isFavorite(1L, 1L));
    }

    @Test
    void testGetFavoriteCount() {
        // Given
        FavoriteRequestDTO request1 = new FavoriteRequestDTO();
        request1.setCustomerId(1L);
        request1.setVenueId(1L);

        FavoriteRequestDTO request2 = new FavoriteRequestDTO();
        request2.setCustomerId(1L);
        request2.setVenueId(2L);

        // When
        favoriteService.createFavorite(request1);
        favoriteService.createFavorite(request2);

        // Then
        long customerCount = favoriteService.getCustomerFavoriteCount(1L);
        assertEquals(2, customerCount);

        long venueCount = favoriteService.getVenueFavoriteCount(1L);
        assertEquals(1, venueCount);
    }
} 