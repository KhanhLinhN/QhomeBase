package com.QhomeBase.marketplaceservice.service;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageKitService {

    private final ImageKit imageKit;

    /**
     * Upload a single image to ImageKit with retry logic
     * @param file The image file to upload
     * @param folder Optional folder path in ImageKit (e.g., "marketplace/posts", "marketplace/comments")
     * @return The URL of the uploaded image
     * @throws IOException if file cannot be read or upload fails after retries
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        int maxRetries = 3;
        long baseDelayMs = 1000; // Start with 1 second delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 [ImageKit] Uploading image (attempt {}/{}): {} to folder: {}", 
                        attempt, maxRetries, file.getOriginalFilename(), folder);
                
                // Convert file to base64
                byte[] fileBytes = file.getBytes();
                String base64File = Base64.getEncoder().encodeToString(fileBytes);
                
                // Generate unique filename
                String originalFileName = file.getOriginalFilename();
                String fileExtension = originalFileName != null && originalFileName.contains(".")
                        ? originalFileName.substring(originalFileName.lastIndexOf("."))
                        : ".jpg";
                String fileName = UUID.randomUUID().toString() + fileExtension;
                
                // Create upload request
                FileCreateRequest fileCreateRequest = new FileCreateRequest(base64File, fileName);
                fileCreateRequest.setUseUniqueFileName(true);
                
                // Set folder path if provided
                if (folder != null && !folder.isEmpty()) {
                    fileCreateRequest.setFolder(folder);
                }
                
                // Upload to ImageKit
                Result result = imageKit.upload(fileCreateRequest);
                
                if (result != null && result.getUrl() != null) {
                    log.info("✅ [ImageKit] Image uploaded successfully (attempt {}): {}", attempt, result.getUrl());
                    return result.getUrl();
                } else {
                    log.error("❌ [ImageKit] Upload failed (attempt {}): result is null or URL is null", attempt);
                    if (attempt == maxRetries) {
                        throw new IOException("Failed to upload image to ImageKit: no URL returned");
                    }
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                boolean isRetryableError = errorMessage != null && (
                        errorMessage.contains("FLOW_CONTROL_ERROR") ||
                        errorMessage.contains("stream was reset") ||
                        errorMessage.contains("Connection") ||
                        errorMessage.contains("timeout") ||
                        errorMessage.contains("network")
                );
                
                if (attempt < maxRetries && isRetryableError) {
                    long delayMs = baseDelayMs * (long) Math.pow(2, attempt - 1); // Exponential backoff
                    log.warn("⚠️ [ImageKit] Upload failed (attempt {}), retrying in {}ms: {}", 
                            attempt, delayMs, errorMessage);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Upload interrupted", ie);
                    }
                } else {
                    log.error("❌ [ImageKit] Error uploading image (attempt {}): {}", attempt, errorMessage, e);
                    throw new IOException("Error uploading image to ImageKit: " + errorMessage, e);
                }
            }
        }
        
        throw new IOException("Failed to upload image to ImageKit after " + maxRetries + " attempts");
    }

    /**
     * Upload multiple images to ImageKit
     * @param files List of image files to upload
     * @param folder Optional folder path in ImageKit
     * @return List of URLs of uploaded images
     * @throws IOException if any file cannot be read or uploaded
     */
    public List<String> uploadImages(List<MultipartFile> files, String folder) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = uploadImage(file, folder);
            imageUrls.add(url);
        }
        log.info("✅ [ImageKit] Uploaded {} images successfully", imageUrls.size());
        return imageUrls;
    }

    /**
     * Upload image from byte array to ImageKit with retry logic
     * @param imageBytes The image bytes to upload
     * @param fileName The filename (with extension) for the image
     * @param folder Optional folder path in ImageKit (e.g., "marketplace/posts", "marketplace/comments")
     * @return The URL of the uploaded image
     * @throws IOException if upload fails after retries
     */
    public String uploadImageFromBytes(byte[] imageBytes, String fileName, String folder) throws IOException {
        int maxRetries = 3;
        long baseDelayMs = 1000; // Start with 1 second delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 [ImageKit] Uploading image from bytes (attempt {}/{}): {} to folder: {}", 
                        attempt, maxRetries, fileName, folder);
                
                // Convert bytes to base64
                String base64File = Base64.getEncoder().encodeToString(imageBytes);
                
                // Ensure filename has extension
                String fileExtension = fileName != null && fileName.contains(".")
                        ? fileName.substring(fileName.lastIndexOf("."))
                        : ".jpg";
                String finalFileName = fileName != null && fileName.contains(".")
                        ? fileName
                        : UUID.randomUUID().toString() + fileExtension;
                
                // Create upload request
                FileCreateRequest fileCreateRequest = new FileCreateRequest(base64File, finalFileName);
                fileCreateRequest.setUseUniqueFileName(true);
                
                // Set folder path if provided
                if (folder != null && !folder.isEmpty()) {
                    fileCreateRequest.setFolder(folder);
                }
                
                // Upload to ImageKit
                Result result = imageKit.upload(fileCreateRequest);
                
                if (result != null && result.getUrl() != null) {
                    log.info("✅ [ImageKit] Image uploaded successfully from bytes (attempt {}): {}", attempt, result.getUrl());
                    return result.getUrl();
                } else {
                    log.error("❌ [ImageKit] Upload failed (attempt {}): result is null or URL is null", attempt);
                    if (attempt == maxRetries) {
                        throw new IOException("Failed to upload image to ImageKit: no URL returned");
                    }
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                boolean isRetryableError = errorMessage != null && (
                        errorMessage.contains("FLOW_CONTROL_ERROR") ||
                        errorMessage.contains("stream was reset") ||
                        errorMessage.contains("Connection") ||
                        errorMessage.contains("timeout") ||
                        errorMessage.contains("network")
                );
                
                if (attempt < maxRetries && isRetryableError) {
                    long delayMs = baseDelayMs * (long) Math.pow(2, attempt - 1); // Exponential backoff
                    log.warn("⚠️ [ImageKit] Upload failed (attempt {}), retrying in {}ms: {}", 
                            attempt, delayMs, errorMessage);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Upload interrupted", ie);
                    }
                } else {
                    log.error("❌ [ImageKit] Error uploading image from bytes (attempt {}): {}", attempt, errorMessage, e);
                    throw new IOException("Error uploading image to ImageKit: " + errorMessage, e);
                }
            }
        }
        
        throw new IOException("Failed to upload image to ImageKit after " + maxRetries + " attempts");
    }
}
