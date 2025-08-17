package com.zanar.playera.controller;

import com.zanar.playera.dto.ReviewRequestDTO;
import com.zanar.playera.dto.ReviewResponseDTO;
import com.zanar.playera.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
  @Autowired
  private ReviewService reviewService;

  @GetMapping
  public ResponseEntity<List<ReviewResponseDTO>> listReviews() {
    return ResponseEntity.ok(reviewService.listReviews());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReviewResponseDTO> getReviewById(@PathVariable Long id) {
    return ResponseEntity.ok(reviewService.getReviewById(id));
  }

  @GetMapping("/venue/{venueId}")
  public ResponseEntity<List<ReviewResponseDTO>> listReviewsByVenue(@PathVariable Long venueId) {
    return ResponseEntity.ok(reviewService.listReviewsByVenue(venueId));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<ReviewResponseDTO>> listReviewsByCustomer(@PathVariable Long customerId) {
    return ResponseEntity.ok(reviewService.listReviewsByCustomer(customerId));
  }

  @PostMapping
  public ResponseEntity<ReviewResponseDTO> createReview(@RequestBody ReviewRequestDTO dto) {
    return ResponseEntity.ok(reviewService.createReview(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ReviewResponseDTO> updateReview(@PathVariable Long id, @RequestBody ReviewRequestDTO dto) {
    return ResponseEntity.ok(reviewService.updateReview(id, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
    reviewService.deleteReview(id);
    return ResponseEntity.noContent().build();
  }
}