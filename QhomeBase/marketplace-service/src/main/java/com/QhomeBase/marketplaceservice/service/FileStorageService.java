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

    /**
     * Upload image and return URLs for different sizes
     * Returns map with keys: original, thumbnail, medium, large
     */
    public Map<String, String> uploadImage(MultipartFile file, String postId) throws IOException {
        log.info("Uploading image for post: {}", postId);
        
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDirectory, postId);
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Map<String, String> imageUrls = new HashMap<>();

        // Upload original
        Path originalPath = uploadPath.resolve("original_" + fileName);
        Files.copy(file.getInputStream(), originalPath, StandardCopyOption.REPLACE_EXISTING);
        imageUrls.put("original", getImageUrl(postId, "original_" + fileName));

        log.info("Uploaded image: {}", originalPath);
        return imageUrls;
    }

    /**
     * Upload processed images (thumbnail, medium, large)
     */
    public Map<String, String> uploadProcessedImages(Map<String, byte[]> processedImages, String postId, String baseFileName) throws IOException {
        log.info("Uploading processed images for post: {}", postId);
        
        Path uploadPath = Paths.get(uploadDirectory, postId);
        Files.createDirectories(uploadPath);

        Map<String, String> imageUrls = new HashMap<>();

        for (Map.Entry<String, byte[]> entry : processedImages.entrySet()) {
            String size = entry.getKey();
            byte[] imageData = entry.getValue();
            
            String fileName = size + "_" + baseFileName;
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, imageData);
            
            imageUrls.put(size, getImageUrl(postId, fileName));
            log.debug("Uploaded {} image: {}", size, filePath);
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
     * Upload comment image
     * Returns the image URL
     */
    public String uploadCommentImage(MultipartFile file, String postId) throws IOException {
        log.info("Uploading comment image for post: {}", postId);
        
        // Create directory if not exists (use comments subdirectory)
        Path uploadPath = Paths.get(uploadDirectory, postId, "comments");
        Files.createDirectories(uploadPath);

        String fileName = UUID.randomUUID().toString() + ".jpg";
        
        // Upload image
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        String imageUrl = getCommentImageUrl(postId, fileName);
        log.info("Uploaded comment image: {}", filePath);
        return imageUrl;
    }

    /**
     * Upload comment video
     * Returns the video URL
     */
    public String uploadCommentVideo(MultipartFile file, String postId) throws IOException {
        log.info("Uploading comment video for post: {}", postId);
        
        // Create directory if not exists (use comments subdirectory)
        Path uploadPath = Paths.get(uploadDirectory, postId, "comments");
        Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".mp4";
        String fileName = UUID.randomUUID().toString() + extension;
        
        // Upload video
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        String videoUrl = getCommentVideoUrl(postId, fileName);
        log.info("Uploaded comment video: {}", filePath);
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

