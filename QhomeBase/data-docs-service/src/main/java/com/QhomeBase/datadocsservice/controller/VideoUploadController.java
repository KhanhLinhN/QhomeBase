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
public class VideoUploadController {

    private final VideoStorageService videoStorageService;

    @PostMapping("/upload")
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
            log.info("Uploading video: category={}, ownerId={}, uploadedBy={}, size={} MB",
                    category, ownerId, uploadedBy, file.getSize() / (1024.0 * 1024.0));
            
            VideoStorage videoStorage = videoStorageService.uploadVideo(
                    file, category, ownerId, uploadedBy, resolution, durationSeconds, width, height);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VideoUploadResponse.from(videoStorage));
        } catch (FileStorageException e) {
            log.error("Error uploading video: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error uploading video", e);
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

    @GetMapping("/stream/{videoId}")
    @Operation(summary = "Stream video", description = "Stream video file by ID")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable UUID videoId,
            HttpServletRequest request) {
        try {
            VideoStorage video = videoStorageService.getVideoById(videoId);
            
            Path videoPath = Paths.get(video.getFilePath());
            Resource resource = new UrlResource(videoPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Video file not found or not readable: {}", video.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            String contentType = video.getContentType();
            if (contentType == null) {
                contentType = "video/mp4";
            }
            
            // Support range requests for video streaming
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader != null) {
                // For now, return full video. Can implement partial content later if needed
                log.debug("Range request received: {}", rangeHeader);
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalFileName() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error streaming video: {}", e.getMessage(), e);
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
