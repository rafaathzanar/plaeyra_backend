package com.zanar.playera.service;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Favorite;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.mapper.FavoriteMapper;
import com.zanar.playera.repo.FavoriteRepository;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
  @Autowired
  private FavoriteRepository favoriteRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private VenueRepository venueRepository;

  public FavoriteResponseDTO createFavorite(FavoriteRequestDTO dto) {
    Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
        .orElseThrow(() -> new RuntimeException("Customer not found"));
    Venue venue = venueRepository.findById(dto.getVenueId())
        .orElseThrow(() -> new RuntimeException("Venue not found"));
    Favorite favorite = FavoriteMapper.toFavoriteEntity(dto, customer, venue);
    Favorite saved = favoriteRepository.save(favorite);
    return FavoriteMapper.toFavoriteResponseDTO(saved);
  }

  public FavoriteResponseDTO getFavoriteById(Long id) {
    Favorite favorite = favoriteRepository.findById(id).orElseThrow(() -> new RuntimeException("Favorite not found"));
    return FavoriteMapper.toFavoriteResponseDTO(favorite);
  }

  public List<FavoriteResponseDTO> listFavorites() {
    return favoriteRepository.findAll().stream().map(FavoriteMapper::toFavoriteResponseDTO)
        .collect(Collectors.toList());
  }

  public List<FavoriteResponseDTO> listFavoritesByCustomer(Long customerId) {
    return favoriteRepository.findAll().stream()
        .filter(f -> f.getCustomer() != null && f.getCustomer().getUserId().equals(customerId))
        .map(FavoriteMapper::toFavoriteResponseDTO)
        .collect(Collectors.toList());
  }

  public void deleteFavorite(Long id) {
    favoriteRepository.deleteById(id);
  }
}