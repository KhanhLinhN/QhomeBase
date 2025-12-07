package com.QhomeBase.marketplaceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * File storage service
 * Currently uses local storage, can be extended to use S3/Firebase Storage
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${marketplace.upload.directory:uploads/marketplace}")
    private String uploadDirectory;

    @Value("${marketplace.cdn.base-url:}")
    private String cdnBaseUrl;

    private final ImageKitService imageKitService;

    public FileStorageService(ImageKitService imageKitService) {
        this.imageKitService = imageKitService;
    }

    /**
     * Upload image to ImageKit and return URL
     * Returns map with key: original (ImageKit URL)
     */
    public Map<String, String> uploadImage(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading image to ImageKit for post: {}", postId);
        
        // Upload to ImageKit with folder "marketplace/posts/{postId}"
        String imageUrl = imageKitService.uploadImage(file, "marketplace/posts/" + postId);

        Map<String, String> imageUrls = new HashMap<>();
        imageUrls.put("original", imageUrl);

        log.info("✅ [FileStorageService] Uploaded image to ImageKit: {}", imageUrl);
        return imageUrls;
    }

    /**
     * Upload processed images (thumbnail, medium, large) to ImageKit
     * Note: For now, we upload the original image only. ImageKit can generate transformations on-the-fly.
     */
    public Map<String, String> uploadProcessedImages(Map<String, byte[]> processedImages, String postId, String baseFileName) throws IOException {
        log.info("📤 [FileStorageService] Uploading processed images to ImageKit for post: {}", postId);
        
        // For ImageKit, we can use transformations. For now, upload original if available
        // In the future, we can upload different sizes or use ImageKit transformations
        Map<String, String> imageUrls = new HashMap<>();

        // Upload original if available
        if (processedImages.containsKey("original")) {
            // Note: This would require converting byte[] back to MultipartFile
            // For now, we'll use the original upload method
            log.warn("⚠️ [FileStorageService] Processed images upload not fully implemented for ImageKit. Using original image.");
        }

        return imageUrls;
    }

    /**
     * Delete image files
     */
    public void deleteImage(String postId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, postId, fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted image: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting image: {}", fileName, e);
        }
    }

    /**
     * Delete all images for a post
     */
    public void deletePostImages(String postId) {
        try {
            Path postPath = Paths.get(uploadDirectory, postId);
            if (Files.exists(postPath)) {
                Files.walk(postPath)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Error deleting file: {}", path, e);
                            }
                        });
                log.info("Deleted all images for post: {}", postId);
            }
        } catch (IOException e) {
            log.error("Error deleting post images: {}", postId, e);
        }
    }

    /**
     * Get image URL (with CDN if configured)
     */
    private String getImageUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            // Use CDN URL
            return String.format("%s/marketplace/%s/%s", cdnBaseUrl, postId, fileName);
        } else {
            // Use local URL (for development)
            return String.format("/api/marketplace/uploads/%s/%s", postId, fileName);
        }
    }

    /**
     * Upload comment image to ImageKit
     * Returns the image URL
     */
    public String uploadCommentImage(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading comment image to ImageKit for post: {}", postId);
        
        // Upload to ImageKit with folder "marketplace/comments/{postId}"
        String imageUrl = imageKitService.uploadImage(file, "marketplace/comments/" + postId);
        
        log.info("✅ [FileStorageService] Uploaded comment image to ImageKit: {}", imageUrl);
        return imageUrl;
    }

    /**
     * Upload comment video to ImageKit
     * Returns the video URL
     */
    public String uploadCommentVideo(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading comment video to ImageKit for post: {}", postId);
        
        // Upload to ImageKit with folder "marketplace/comments/{postId}"
        String videoUrl = imageKitService.uploadImage(file, "marketplace/comments/" + postId);
        
        log.info("✅ [FileStorageService] Uploaded comment video to ImageKit: {}", videoUrl);
        return videoUrl;
    }

    /**
     * Get comment image URL
     */
    private String getCommentImageUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return String.format("%s/marketplace/%s/comments/%s", cdnBaseUrl, postId, fileName);
        } else {
            return String.format("/api/marketplace/uploads/%s/comments/%s", postId, fileName);
        }
    }

    /**
     * Get comment video URL
     */
    private String getCommentVideoUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return String.format("%s/marketplace/%s/comments/%s", cdnBaseUrl, postId, fileName);
        } else {
            return String.format("/api/marketplace/uploads/%s/comments/%s", postId, fileName);
        }
    }

    /**
     * Get image file path
     */
    public Path getImagePath(String postId, String fileName) {
        Path path = Paths.get(uploadDirectory, postId, fileName);
        log.debug("📁 [FileStorageService] Getting image path: postId={}, fileName={}, fullPath={}, exists={}", 
                postId, fileName, path, java.nio.file.Files.exists(path));
        return path;
    }

    /**
     * Get comment image/video file path
     */
    public Path getCommentFilePath(String postId, String fileName) {
        Path path = Paths.get(uploadDirectory, postId, "comments", fileName);
        log.debug("📁 [FileStorageService] Getting comment file path: postId={}, fileName={}, fullPath={}, exists={}", 
                postId, fileName, path, java.nio.file.Files.exists(path));
        return path;
    }
}

