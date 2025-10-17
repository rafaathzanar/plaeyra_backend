package com.zanar.playera.controller;

import com.zanar.playera.dto.*;
import com.zanar.playera.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loyalty Program", description = "Customer loyalty program management")
public class LoyaltyController {

  private final LoyaltyService loyaltyService;

  @GetMapping("/profile")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Get customer loyalty information", description = "Retrieves comprehensive loyalty information including points, gold coins, tier, and benefits")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Loyalty information retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<LoyaltyInfoDTO> getLoyaltyProfile(@RequestParam Long customerId) {
    log.info("Getting loyalty profile for customer: {}", customerId);
    LoyaltyInfoDTO loyaltyInfo = loyaltyService.getLoyaltyInfo(customerId);
    return ResponseEntity.ok(loyaltyInfo);
  }

  @PostMapping("/earn/booking")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Earn loyalty rewards from booking", description = "Processes loyalty point and gold coin earning from a completed booking")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Loyalty rewards processed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<LoyaltyEarningDTO> earnFromBooking(
      @RequestParam Long customerId,
      @RequestParam Double bookingAmount,
      @RequestParam String bookingId) {
    log.info("Processing loyalty earning from booking - Customer: {}, Amount: {}, Booking: {}",
        customerId, bookingAmount, bookingId);
    LoyaltyEarningDTO earning = loyaltyService.earnFromBooking(customerId, bookingAmount, bookingId);
    return ResponseEntity.ok(earning);
  }

  @PostMapping("/earn/review")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Earn loyalty rewards from review", description = "Processes loyalty point and gold coin earning from a submitted review")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Loyalty rewards processed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<LoyaltyEarningDTO> earnFromReview(
      @RequestParam Long customerId,
      @RequestParam String reviewId) {
    log.info("Processing loyalty earning from review - Customer: {}, Review: {}", customerId, reviewId);
    LoyaltyEarningDTO earning = loyaltyService.earnFromReview(customerId, reviewId);
    return ResponseEntity.ok(earning);
  }

  @PostMapping("/redeem/gold-coins")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Redeem gold coins for discount", description = "Redeems gold coins to get discount on booking")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Gold coins redeemed successfully"),
      @ApiResponse(responseCode = "400", description = "Insufficient gold coins or invalid request"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<GoldCoinRedemptionDTO> redeemGoldCoins(
      @RequestParam Long customerId,
      @RequestParam Integer coinsToRedeem,
      @RequestParam String bookingReference) {
    log.info("Processing gold coin redemption - Customer: {}, Coins: {}, Booking: {}",
        customerId, coinsToRedeem, bookingReference);
    GoldCoinRedemptionDTO redemption = loyaltyService.redeemGoldCoins(customerId, coinsToRedeem, bookingReference);
    return ResponseEntity.ok(redemption);
  }

  @GetMapping("/calculate-discount")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Calculate available discount", description = "Calculates the maximum discount available for a customer based on tier and gold coins")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Discount calculated successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Double> calculateAvailableDiscount(
      @RequestParam Long customerId,
      @RequestParam Double bookingAmount) {
    log.info("Calculating available discount - Customer: {}, Amount: {}", customerId, bookingAmount);
    Double discount = loyaltyService.calculateAvailableDiscount(customerId, bookingAmount);
    return ResponseEntity.ok(discount);
  }

  @PostMapping("/test-earn")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Test loyalty earning", description = "Manually trigger loyalty earning for testing")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Loyalty earning tested successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - admin only")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<String> testLoyaltyEarning(@RequestParam Long customerId, @RequestParam Double amount) {
    log.info("Testing loyalty earning for customer: {}, amount: {}", customerId, amount);
    try {
      LoyaltyEarningDTO result = loyaltyService.earnFromBooking(customerId, amount,
          "TEST-" + System.currentTimeMillis());
      return ResponseEntity.ok("Loyalty earning test successful: " + result.toString());
    } catch (Exception e) {
      log.error("Error testing loyalty earning: {}", e.getMessage());
      return ResponseEntity.badRequest().body("Error testing loyalty earning: " + e.getMessage());
    }
  }

  @PostMapping("/fix-gold-coins")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Fix null gold coins", description = "Updates all customers with null goldCoins to 0")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Gold coins fixed successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - admin only")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<String> fixGoldCoins() {
    log.info("Fixing null gold coins for all customers");
    try {
      loyaltyService.fixNullGoldCoins();
      return ResponseEntity.ok("Gold coins fixed successfully");
    } catch (Exception e) {
      log.error("Error fixing gold coins: {}", e.getMessage());
      return ResponseEntity.badRequest().body("Error fixing gold coins: " + e.getMessage());
    }
  }

  @GetMapping("/transactions")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Get loyalty transaction history", description = "Retrieves the loyalty transaction history for a customer")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Customer not found")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<List<LoyaltyTransactionDTO>> getTransactionHistory(
      @RequestParam Long customerId,
      @RequestParam(defaultValue = "10") int limit) {
    log.info("Getting transaction history for customer: {}, limit: {}", customerId, limit);
    List<LoyaltyTransactionDTO> transactions = loyaltyService.getTransactionHistory(customerId, limit);
    return ResponseEntity.ok(transactions);
  }
}
