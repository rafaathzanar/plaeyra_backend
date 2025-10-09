package com.zanar.playera.controller;

import com.zanar.playera.service.GoogleCloudStorageService;
import com.zanar.playera.service.VenueService;
import com.zanar.playera.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Management", description = "APIs for uploading and managing images")
@CrossOrigin(origins = "*")
public class ImageController {

  @Autowired
  private GoogleCloudStorageService storageService;

  @Autowired
  private VenueService venueService;

  @Autowired
  private CourtService courtService;

  @PostMapping("/venue/{venueId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Upload venue images", description = "Upload multiple images for a venue")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file format or empty files"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> uploadVenueImages(
      @PathVariable Long venueId,
      @RequestParam("files") MultipartFile[] files) {

    try {
      // Validate files
      if (files == null || files.length == 0) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "No files provided");
        return ResponseEntity.badRequest().body(response);
      }

      // Upload images to Google Cloud Storage
      String[] imageUrls = storageService.uploadImages(files, "venues");

      // Update venue with new image URLs
      venueService.addImagesToVenue(venueId, imageUrls);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Images uploaded successfully");
      response.put("imageUrls", imageUrls);
      response.put("count", imageUrls.length);

      return ResponseEntity.ok(response);

    } catch (IOException e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Failed to upload images: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Error processing request: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

  @PostMapping("/court/{courtId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Upload court images", description = "Upload multiple images for a court")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Images uploaded successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid file format or empty files"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> uploadCourtImages(
      @PathVariable Long courtId,
      @RequestParam("files") MultipartFile[] files) {

    try {
      // Validate files
      if (files == null || files.length == 0) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "No files provided");
        return ResponseEntity.badRequest().body(response);
      }

      // Upload images to Google Cloud Storage
      String[] imageUrls = storageService.uploadImages(files, "courts");

      // Update court with new image URLs
      courtService.addImagesToCourt(courtId, imageUrls);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Images uploaded successfully");
      response.put("imageUrls", imageUrls);
      response.put("count", imageUrls.length);

      return ResponseEntity.ok(response);

    } catch (IOException e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Failed to upload images: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Error processing request: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

  @DeleteMapping("/venue/{venueId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Delete venue image", description = "Delete a specific image from a venue")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> deleteVenueImage(
      @PathVariable Long venueId,
      @RequestParam String imageUrl) {

    try {
      boolean deleted = storageService.deleteImage(imageUrl);
      if (deleted) {
        venueService.removeImageFromVenue(venueId, imageUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Image deleted successfully");
        return ResponseEntity.ok(response);
      } else {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Failed to delete image");
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Error deleting image: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

  @DeleteMapping("/court/{courtId}")
  @PreAuthorize("hasRole('VENUE_OWNER') or hasRole('ADMIN')")
  @Operation(summary = "Delete court image", description = "Delete a specific image from a court")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Map<String, Object>> deleteCourtImage(
      @PathVariable Long courtId,
      @RequestParam String imageUrl) {

    try {
      boolean deleted = storageService.deleteImage(imageUrl);
      if (deleted) {
        courtService.removeImageFromCourt(courtId, imageUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Image deleted successfully");
        return ResponseEntity.ok(response);
      } else {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Failed to delete image");
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Error deleting image: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }
}
