package com.QhomeBase.datadocsservice.service;

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
     * Upload a single image to ImageKit
     * @param file The image file to upload
     * @param folder Optional folder path in ImageKit (e.g., "household", "vehicle", "chat")
     * @return The URL of the uploaded image
     * @throws IOException if file cannot be read
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        try {
            log.info("📤 [ImageKit] Uploading image: {} to folder: {}", file.getOriginalFilename(), folder);
            
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
                log.info("✅ [ImageKit] Image uploaded successfully: {}", result.getUrl());
                return result.getUrl();
            } else {
                log.error("❌ [ImageKit] Upload failed: result is null or URL is null");
                throw new IOException("Failed to upload image to ImageKit");
            }
        } catch (Exception e) {
            log.error("❌ [ImageKit] Error uploading image: {}", e.getMessage(), e);
            throw new IOException("Error uploading image to ImageKit: " + e.getMessage(), e);
        }
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
     * Upload a file (image, video, document) to ImageKit
     * @param file The file to upload
     * @param folder Optional folder path in ImageKit
     * @return The URL of the uploaded file
     * @throws IOException if file cannot be read
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        return uploadImage(file, folder); // Same implementation for now
    }
}
