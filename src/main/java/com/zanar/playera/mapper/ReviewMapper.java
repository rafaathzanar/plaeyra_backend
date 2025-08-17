package com.zanar.playera.mapper;

import org.springframework.stereotype.Component;

import com.zanar.playera.dto.ReviewRequestDTO;
import com.zanar.playera.dto.ReviewResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Review;
import com.zanar.playera.entity.Venue;

@Component
public class ReviewMapper {
  public static Review toReviewEntity(ReviewRequestDTO dto, Customer customer, Venue venue) {
    Review review = new Review();
    review.setComment(dto.getComment());
    review.setRating(dto.getRating());
    review.setReviewDate(dto.getReviewDate());
    review.setCustomer(customer);
    review.setVenue(venue);
    return review;
  }

  public static ReviewResponseDTO toReviewResponseDTO(Review review) {
    ReviewResponseDTO dto = new ReviewResponseDTO();
    dto.setReviewId(review.getReviewId());
    dto.setComment(review.getComment());
    dto.setRating(review.getRating());
    dto.setReviewDate(review.getReviewDate());
    if (review.getCustomer() != null) {
      dto.setCustomerId(review.getCustomer().getUserId());
      dto.setCustomerName(review.getCustomer().getName());
    }
    if (review.getVenue() != null) {
      dto.setVenueId(review.getVenue().getVenueId());
      dto.setVenueName(review.getVenue().getName());
    }
    return dto;
  }
}