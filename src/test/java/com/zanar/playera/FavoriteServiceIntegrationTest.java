package com.zanar.playera;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import com.zanar.playera.service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FavoriteServiceIntegrationTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

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
        testCustomer = (Customer) userRepository.save(testCustomer);

        // Create test venue owner
        VenueOwner venueOwner = new VenueOwner();
        venueOwner.setName("Test Owner");
        venueOwner.setEmail("owner@example.com");
        venueOwner.setPassword("password");
        venueOwner.setPhone("0987654321");
        venueOwner = (VenueOwner) userRepository.save(venueOwner);

        // Create test venue
        testVenue = new Venue();
        testVenue.setName("Test Venue");
        testVenue.setLocation("Test Location");
        testVenue.setDescription("Test Description");
        testVenue.setContactNo("1234567890");
        testVenue.setVenueOwner(venueOwner);
        testVenue = venueRepository.save(testVenue);
    }

    @Test
    void testAddToFavorites() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(testCustomer.getUserId());
        request.setVenueId(testVenue.getVenueId());

        // When
        FavoriteResponseDTO response = favoriteService.addToFavorites(request);

        // Then
        assertNotNull(response);
        assertEquals(testCustomer.getUserId(), response.getCustomerId());
        assertEquals(testCustomer.getName(), response.getCustomerName());
        assertEquals(testVenue.getVenueId(), response.getVenueId());
        assertEquals(testVenue.getName(), response.getVenueName());
        assertNotNull(response.getAddedDate());
    }

    @Test
    void testAddToFavorites_DuplicateFavorite_ThrowsException() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(testCustomer.getUserId());
        request.setVenueId(testVenue.getVenueId());

        // Add favorite first time
        favoriteService.addToFavorites(request);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            favoriteService.addToFavorites(request);
        });
    }

    @Test
    void testIsVenueInFavorites() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(testCustomer.getUserId());
        request.setVenueId(testVenue.getVenueId());

        // When - before adding to favorites
        boolean isFavoriteBefore = favoriteService.isVenueInFavorites(testCustomer.getUserId(), testVenue.getVenueId());

        // Add to favorites
        favoriteService.addToFavorites(request);

        // When - after adding to favorites
        boolean isFavoriteAfter = favoriteService.isVenueInFavorites(testCustomer.getUserId(), testVenue.getVenueId());

        // Then
        assertFalse(isFavoriteBefore);
        assertTrue(isFavoriteAfter);
    }

    @Test
    void testToggleFavorite() {
        // Given
        Long customerId = testCustomer.getUserId();
        Long venueId = testVenue.getVenueId();

        // When - first toggle (should add to favorites)
        FavoriteResponseDTO result1 = favoriteService.toggleFavorite(customerId, venueId);

        // Then
        assertNotNull(result1);
        assertEquals(customerId, result1.getCustomerId());
        assertEquals(venueId, result1.getVenueId());

        // When - second toggle (should remove from favorites)
        FavoriteResponseDTO result2 = favoriteService.toggleFavorite(customerId, venueId);

        // Then
        assertNull(result2);
        assertFalse(favoriteService.isVenueInFavorites(customerId, venueId));
    }

    @Test
    void testGetCustomerFavoriteCount() {
        // Given
        FavoriteRequestDTO request = new FavoriteRequestDTO();
        request.setCustomerId(testCustomer.getUserId());
        request.setVenueId(testVenue.getVenueId());

        // When - before adding to favorites
        long countBefore = favoriteService.getCustomerFavoriteCount(testCustomer.getUserId());

        // Add to favorites
        favoriteService.addToFavorites(request);

        // When - after adding to favorites
        long countAfter = favoriteService.getCustomerFavoriteCount(testCustomer.getUserId());

        // Then
        assertEquals(0, countBefore);
        assertEquals(1, countAfter);
    }
} 