package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "File Uploads", description = "APIs for serving uploaded images")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{postId}/{fileName:.+}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get image", description = "Get uploaded image file")
    public ResponseEntity<Resource> getImage(
            @PathVariable String postId,
            @PathVariable String fileName) {
        
        log.info("📸 [FileUploadController] Request to get image: postId={}, fileName={}", postId, fileName);
        
        try {
            Path filePath = fileStorageService.getImagePath(postId, fileName);
            log.info("📸 [FileUploadController] Image path: {}", filePath);
            log.info("📸 [FileUploadController] File exists: {}", java.nio.file.Files.exists(filePath));
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                log.info("✅ [FileUploadController] Serving image: postId={}, fileName={}, size={}", 
                        postId, fileName, resource.contentLength());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // Cache for 1 year
                        .body(resource);
            } else {
                log.warn("⚠️ [FileUploadController] Image not found or not readable: postId={}, fileName={}, path={}", 
                        postId, fileName, filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ [FileUploadController] Error serving image: postId={}, fileName={}", postId, fileName, e);
            return ResponseEntity.notFound().build();
        }
    }
}

