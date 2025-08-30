package com.zanar.playera.mapper;

import org.springframework.stereotype.Component;

import com.zanar.playera.dto.UserRegistrationDTO;
import com.zanar.playera.dto.UserResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.User;
import com.zanar.playera.entity.VenueOwner;
import com.zanar.playera.entity.User.UserRole;

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
      // Set the role from DTO
      if (dto.getRole() != null) {
        try {
          customer.setRole(UserRole.valueOf(dto.getRole()));
        } catch (IllegalArgumentException e) {
          // Default to CUSTOMER if role is invalid
          customer.setRole(UserRole.CUSTOMER);
        }
      } else {
        customer.setRole(UserRole.CUSTOMER);
      }
      return customer;
    } else {
      VenueOwner owner = new VenueOwner();
      owner.setName(dto.getName());
      owner.setEmail(dto.getEmail());
      owner.setPassword(dto.getPassword());
      owner.setPhone(dto.getPhone());
      // Set the role from DTO
      if (dto.getRole() != null) {
        try {
          owner.setRole(UserRole.valueOf(dto.getRole()));
        } catch (IllegalArgumentException e) {
          // Default to VENUE_OWNER if role is invalid
          owner.setRole(UserRole.VENUE_OWNER);
        }
      } else {
        owner.setRole(UserRole.VENUE_OWNER);
      }
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