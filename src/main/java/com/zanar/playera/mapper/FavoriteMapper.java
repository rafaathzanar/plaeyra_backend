package com.zanar.playera.mapper;

import org.springframework.stereotype.Component;

import com.zanar.playera.dto.FavoriteRequestDTO;
import com.zanar.playera.dto.FavoriteResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Favorite;
import com.zanar.playera.entity.Venue;

@Component
public class FavoriteMapper {
  public static Favorite toFavoriteEntity(FavoriteRequestDTO dto, Customer customer, Venue venue) {
    Favorite favorite = new Favorite();
    favorite.setAddedDate(dto.getAddedDate());
    favorite.setCustomer(customer);
    favorite.setVenue(venue);
    return favorite;
  }

  public static FavoriteResponseDTO toFavoriteResponseDTO(Favorite favorite) {
    FavoriteResponseDTO dto = new FavoriteResponseDTO();
    dto.setFavoriteId(favorite.getFavoriteId());
    dto.setAddedDate(favorite.getAddedDate());
    if (favorite.getCustomer() != null) {
      dto.setCustomerId(favorite.getCustomer().getUserId());
      dto.setCustomerName(favorite.getCustomer().getName());
    }
    if (favorite.getVenue() != null) {
      dto.setVenueId(favorite.getVenue().getVenueId());
      dto.setVenueName(favorite.getVenue().getName());
    }
    return dto;
  }
}