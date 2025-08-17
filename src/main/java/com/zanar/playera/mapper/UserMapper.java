package com.zanar.playera.mapper;

import org.springframework.stereotype.Component;

import com.zanar.playera.dto.UserRegistrationDTO;
import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.User;
import com.zanar.playera.entity.VenueOwner;

@Component
public class UserMapper {
  public static User toUserEntity(UserRegistrationDTO dto) {
    if (dto.getUserType() != null && dto.getUserType().equalsIgnoreCase("CUSTOMER")) {
      Customer customer = new Customer();
      customer.setName(dto.getName());
      customer.setEmail(dto.getEmail());
      customer.setPassword(dto.getPassword());
      customer.setPhone(dto.getPhone());
      customer.setLoyaltyPoints(dto.getLoyaltyPoints() != null ? dto.getLoyaltyPoints() : 0);
      return customer;
    } else {
      VenueOwner owner = new VenueOwner();
      owner.setName(dto.getName());
      owner.setEmail(dto.getEmail());
      owner.setPassword(dto.getPassword());
      owner.setPhone(dto.getPhone());
      return owner;
    }
  }

  public static UserResponseDTO toUserResponseDTO(User user) {
    UserResponseDTO dto = new UserResponseDTO();
    dto.setUserId(user.getUserId());
    dto.setName(user.getName());
    dto.setEmail(user.getEmail());
    dto.setPhone(user.getPhone());
    if (user instanceof Customer customer) {
      dto.setUserType("CUSTOMER");
      dto.setLoyaltyPoints(customer.getLoyaltyPoints());
    } else if (user instanceof VenueOwner) {
      dto.setUserType("VENUE_OWNER");
      dto.setLoyaltyPoints(null);
    }
    return dto;
  }
}