package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/uploads/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload APIs for chat")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/{groupId}/image")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload image for chat message", description = "Upload an image file for a chat message")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Upload image
            String imageUrl = fileStorageService.uploadImage(file, groupId);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        }
    }

    @PostMapping("/{groupId}/file")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload file for chat message", description = "Upload a file for a chat message")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Upload file
            String fileUrl = fileStorageService.uploadFile(file, groupId);
            String fileName = file.getOriginalFilename();

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", fileName != null ? fileName : "file");
            response.put("fileSize", String.valueOf(file.getSize()));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }
}

