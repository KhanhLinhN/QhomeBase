package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.VideoUploadResponse;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import com.QhomeBase.datadocsservice.model.VideoStorage;
import com.QhomeBase.datadocsservice.service.VideoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video Upload", description = "Video upload and management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoUploadController {

    private final VideoStorageService videoStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload video", description = "Upload a video file and store in database")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "ownerId", required = false) UUID ownerId,
            @RequestParam("uploadedBy") UUID uploadedBy,
            @RequestParam(value = "resolution", required = false) String resolution,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height) {
        
        try {
            log.info("🔍 [VideoUploadController] Received upload request: category={}, ownerId={}, uploadedBy={}, fileName={}, size={} MB",
                    category, ownerId, uploadedBy, file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0));
            
            VideoStorage videoStorage = videoStorageService.uploadVideo(
                    file, category, ownerId, uploadedBy, resolution, durationSeconds, width, height);
            
            log.info("✅ [VideoUploadController] Video uploaded successfully: videoId={}, fileUrl={}", 
                    videoStorage.getId(), videoStorage.getFileUrl());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VideoUploadResponse.from(videoStorage));
        } catch (FileStorageException e) {
            log.error("❌ [VideoUploadController] FileStorageException: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [VideoUploadController] Unexpected error uploading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{videoId}")
    @Operation(summary = "Get video metadata", description = "Get video metadata by ID")
    public ResponseEntity<VideoUploadResponse> getVideo(@PathVariable UUID videoId) {
        try {
            VideoStorage video = videoStorageService.getVideoById(videoId);
            return ResponseEntity.ok(VideoUploadResponse.from(video));
        } catch (FileStorageException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/category/{category}/owner/{ownerId}")
    @Operation(summary = "Get videos by category and owner", description = "Get all videos for a specific category and owner")
    public ResponseEntity<List<VideoUploadResponse>> getVideosByCategoryAndOwner(
            @PathVariable String category,
            @PathVariable UUID ownerId) {
        try {
            List<VideoStorage> videos = videoStorageService.getVideosByCategoryAndOwner(category, ownerId);
            List<VideoUploadResponse> responses = videos.stream()
                    .map(VideoUploadResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/stream/{videoId}", method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS})
    @Operation(summary = "Stream video", description = "Stream video file by ID - Public access for video playback")
    public ResponseEntity<?> streamVideo(
            @PathVariable UUID videoId,
            HttpServletRequest request) {
        
        // Handle CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
                    .build();
        }
        
        // Handle HEAD requests
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            try {
                VideoStorage video = videoStorageService.getVideoById(videoId);
                Path videoPath = Paths.get(video.getFilePath());
                Resource resource = new UrlResource(videoPath.toUri());
                
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.notFound().build();
                }
                
                String contentType = video.getContentType();
                if (contentType == null) {
                    contentType = "video/mp4";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(video.getFileSize()))
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                        .build();
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            log.info("🔍 [VideoUploadController] Stream request received: videoId={}, Range={}, User-Agent={}, Origin={}", 
                    videoId, 
                    request.getHeader(HttpHeaders.RANGE),
                    request.getHeader(HttpHeaders.USER_AGENT),
                    request.getHeader(HttpHeaders.ORIGIN));
            
            VideoStorage video = videoStorageService.getVideoById(videoId);
            
            Path videoPath = Paths.get(video.getFilePath());
            Resource resource = new UrlResource(videoPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("⚠️ [VideoUploadController] Video file not found or not readable: {}", video.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            String contentType = video.getContentType();
            if (contentType == null) {
                contentType = "video/mp4";
            }
            
            // Support range requests for video streaming
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalFileName() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    // CORS headers for ExoPlayer
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
            
            // Handle range requests for video streaming
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    long fileSize = resource.contentLength();
                    String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                    long start = Long.parseLong(ranges[0]);
                    long end = ranges.length > 1 && !ranges[1].isEmpty() 
                            ? Long.parseLong(ranges[1]) 
                            : fileSize - 1;
                    
                    if (start > end || start < 0 || end >= fileSize) {
                        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                                .build();
                    }
                    
                    long contentLength = end - start + 1;
                    
                    log.debug("📹 [VideoUploadController] Range request: {}-{} of {}", start, end, fileSize);
                    
                    // Return partial content response
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalFileName() + "\"")
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                            .header(HttpHeaders.CONTENT_RANGE, 
                                    String.format("bytes %d-%d/%d", start, end, fileSize))
                            .contentLength(contentLength)
                            .body(resource);
                } catch (Exception e) {
                    log.warn("⚠️ [VideoUploadController] Invalid range header: {}", rangeHeader);
                }
            }
            
            return responseBuilder.body(resource);
        } catch (FileStorageException e) {
            log.error("❌ [VideoUploadController] Video not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [VideoUploadController] Error streaming video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "Delete video", description = "Soft delete a video by ID")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID videoId) {
        try {
            videoStorageService.deleteVideo(videoId);
            return ResponseEntity.noContent().build();
        } catch (FileStorageException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
