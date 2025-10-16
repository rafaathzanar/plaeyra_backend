package com.zanar.playera.service;

import com.google.cloud.storage.*;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Service
public class GoogleCloudStorageService {

  @Value("${gcs.bucket-name}")
  private String bucketName;

  @Value("${gcs.project-id}")
  private String projectId;

  @Value("${gcs.credentials-path}")
  private String credentialsPath;

  private Storage storage;

  @javax.annotation.PostConstruct
  public void initializeStorage() {
    try {
      System.out.println("Initializing Google Cloud Storage...");
      System.out.println("Bucket Name: " + bucketName);
      System.out.println("Project ID: " + projectId);
      System.out.println("Credentials Path: " + credentialsPath);

      // Initialize storage client with credentials file
      if (credentialsPath != null && credentialsPath.startsWith("classpath:")) {
        // For classpath resources
        this.storage = StorageOptions.getDefaultInstance().getService();
      } else if (credentialsPath != null && !credentialsPath.isEmpty()) {
        // For file system paths
        this.storage = StorageOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(ServiceAccountCredentials.fromStream(
                new FileInputStream(credentialsPath)))
            .build()
            .getService();
      } else {
        // Fallback to default instance
        this.storage = StorageOptions.getDefaultInstance().getService();
      }

      System.out.println("Google Cloud Storage initialized successfully");
    } catch (Exception e) {
      System.err.println("Error initializing Google Cloud Storage: " + e.getMessage());
      e.printStackTrace();
      // Fallback to default instance
      try {
        this.storage = StorageOptions.getDefaultInstance().getService();
        System.out.println("Using default Google Cloud Storage instance");
      } catch (Exception fallbackError) {
        System.err.println("Failed to initialize default storage: " + fallbackError.getMessage());
      }
    }
  }

  /**
   * Upload an image file to Google Cloud Storage
   * 
   * @param file   The multipart file to upload
   * @param folder The folder path (e.g., "venues", "courts")
   * @return The public URL of the uploaded file
   */
  public String uploadImage(MultipartFile file, String folder) throws IOException {
    // Validate file
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    // Validate file type
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("File must be an image");
    }

    // Check if storage is initialized
    if (storage == null) {
      throw new IllegalStateException("Google Cloud Storage is not initialized");
    }

    // Generate unique filename
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String filename = folder + "/" + UUID.randomUUID().toString() + extension;

    // Upload to GCS
    BlobId blobId = BlobId.of(bucketName, filename);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
        .setContentType(contentType)
        .build();

    storage.create(blobInfo, file.getBytes());

    // Generate signed URL (valid for 1 hour)
    try {
      URL signedUrl = storage.signUrl(blobInfo, 1, java.util.concurrent.TimeUnit.HOURS);
      return signedUrl.toString();
    } catch (Exception e) {
      System.err.println("Error generating signed URL: " + e.getMessage());
      // Fallback to public URL
      return String.format("https://storage.googleapis.com/%s/%s", bucketName, filename);
    }
  }

  /**
   * Delete an image from Google Cloud Storage
   * 
   * @param imageUrl The public URL of the image to delete
   * @return true if deletion was successful
   */
  public boolean deleteImage(String imageUrl) {
    try {
      // Check if storage is initialized
      if (storage == null) {
        System.err.println("Google Cloud Storage is not initialized");
        return false;
      }

      // Extract filename from URL
      String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
      String folder = imageUrl.substring(imageUrl.indexOf(bucketName) + bucketName.length() + 1);
      folder = folder.substring(0, folder.lastIndexOf("/"));
      String fullPath = folder + "/" + filename;

      BlobId blobId = BlobId.of(bucketName, fullPath);
      return storage.delete(blobId);
    } catch (Exception e) {
      System.err.println("Error deleting image: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Upload multiple images
   * 
   * @param files  Array of multipart files
   * @param folder The folder path
   * @return Array of public URLs
   */
  public String[] uploadImages(MultipartFile[] files, String folder) throws IOException {
    String[] urls = new String[files.length];
    for (int i = 0; i < files.length; i++) {
      urls[i] = uploadImage(files[i], folder);
    }
    return urls;
  }
}
