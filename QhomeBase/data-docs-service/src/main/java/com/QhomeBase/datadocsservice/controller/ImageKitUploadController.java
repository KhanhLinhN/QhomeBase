package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.service.ImageKitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/imagekit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ImageKit Upload", description = "APIs for uploading images/files to ImageKit")
public class ImageKitUploadController {

    private final ImageKitService imageKitService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload single image/file to ImageKit", description = "Upload a single image or file to ImageKit")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        
        try {
            log.info("📤 [ImageKitUpload] Uploading file: {} to folder: {}", file.getOriginalFilename(), folder);
            String imageUrl = imageKitService.uploadImage(file, folder);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("fileName", file.getOriginalFilename());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            log.error("❌ [ImageKitUpload] Error uploading file: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload multiple images/files to ImageKit", description = "Upload multiple images or files to ImageKit")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folder) {
        
        try {
            log.info("📤 [ImageKitUpload] Uploading {} files to folder: {}", files.length, folder);
            List<String> imageUrls = imageKitService.uploadImages(List.of(files), folder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("urls", imageUrls);
            response.put("count", imageUrls.size());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            log.error("❌ [ImageKitUpload] Error uploading files: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
