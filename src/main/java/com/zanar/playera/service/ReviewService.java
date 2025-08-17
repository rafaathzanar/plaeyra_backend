package com.zanar.playera.service;

import com.zanar.playera.dto.ReviewRequestDTO;
import com.zanar.playera.dto.ReviewResponseDTO;
import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.Review;
import com.zanar.playera.entity.Venue;
import com.zanar.playera.mapper.ReviewMapper;
import com.zanar.playera.repo.ReviewRepository;
import com.zanar.playera.repo.UserRepository;
import com.zanar.playera.repo.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
  @Autowired
  private ReviewRepository reviewRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private VenueRepository venueRepository;

  public ReviewResponseDTO createReview(ReviewRequestDTO dto) {
    Customer customer = (Customer) userRepository.findById(dto.getCustomerId())
        .orElseThrow(() -> new RuntimeException("Customer not found"));
    Venue venue = venueRepository.findById(dto.getVenueId())
        .orElseThrow(() -> new RuntimeException("Venue not found"));
    Review review = ReviewMapper.toReviewEntity(dto, customer, venue);
    Review saved = reviewRepository.save(review);
    return ReviewMapper.toReviewResponseDTO(saved);
  }

  public ReviewResponseDTO getReviewById(Long id) {
    Review review = reviewRepository.findById(id).orElseThrow(() -> new RuntimeException("Review not found"));
    return ReviewMapper.toReviewResponseDTO(review);
  }

  public List<ReviewResponseDTO> listReviews() {
    return reviewRepository.findAll().stream().map(ReviewMapper::toReviewResponseDTO).collect(Collectors.toList());
  }

  public List<ReviewResponseDTO> listReviewsByVenue(Long venueId) {
    return reviewRepository.findAll().stream()
        .filter(r -> r.getVenue() != null && r.getVenue().getVenueId().equals(venueId))
        .map(ReviewMapper::toReviewResponseDTO)
        .collect(Collectors.toList());
  }

  public List<ReviewResponseDTO> listReviewsByCustomer(Long customerId) {
    return reviewRepository.findAll().stream()
        .filter(r -> r.getCustomer() != null && r.getCustomer().getUserId().equals(customerId))
        .map(ReviewMapper::toReviewResponseDTO)
        .collect(Collectors.toList());
  }

  public ReviewResponseDTO updateReview(Long id, ReviewRequestDTO dto) {
    Review review = reviewRepository.findById(id).orElseThrow(() -> new RuntimeException("Review not found"));
    review.setComment(dto.getComment());
    review.setRating(dto.getRating());
    review.setReviewDate(dto.getReviewDate());
    Review saved = reviewRepository.save(review);
    return ReviewMapper.toReviewResponseDTO(saved);
  }

  public void deleteReview(Long id) {
    reviewRepository.deleteById(id);
  }
}