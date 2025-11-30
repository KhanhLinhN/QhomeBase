package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.dto.*;
import com.QhomeBase.marketplaceservice.mapper.MarketplaceMapper;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostImageRepository;
import com.QhomeBase.marketplaceservice.security.UserPrincipal;
import com.QhomeBase.marketplaceservice.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Marketplace Posts", description = "APIs for managing marketplace posts")
public class MarketplacePostController {
    
    private final ObjectMapper objectMapper;

    private final MarketplacePostService postService;
    private final MarketplaceNotificationService notificationService;
    private final MarketplaceLikeService likeService;
    private final MarketplaceCategoryService categoryService;
    private final RateLimitService rateLimitService;
    private final ImageProcessingService imageProcessingService;
    private final FileStorageService fileStorageService;
    private final AsyncImageProcessingService asyncImageProcessingService;
    private final MarketplaceMapper mapper;
    private final ResidentInfoService residentInfoService;
    private final MarketplacePostImageRepository imageRepository;

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get posts with filters", description = "Get paginated list of posts with optional filters")
    public ResponseEntity<PostPagedResponse> getPosts(
            @RequestParam UUID buildingId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
        Page<MarketplacePost> posts = postService.getPosts(
                buildingId, postStatus, category, minPrice, maxPrice, search, sortBy, page, size
        );

        // Check liked status for each post if user is authenticated
        // Note: Public access - anyone can view posts, but only authenticated users can see their like status
        List<Boolean> likedStatuses = new ArrayList<>();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            String accessToken = principal.token();
            
            // Get residentId from userId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId != null) {
                final UUID finalResidentId = residentId;
                likedStatuses = posts.getContent().stream()
                        .map(post -> likeService.isLikedByUser(post.getId(), finalResidentId))
                        .collect(Collectors.toList());
            } else {
                // If cannot get residentId, set all to false
                likedStatuses = posts.getContent().stream()
                        .map(post -> false)
                        .collect(Collectors.toList());
            }
        } else {
            // Not authenticated - all posts are not liked
            likedStatuses = posts.getContent().stream()
                    .map(post -> false)
                    .collect(Collectors.toList());
        }

        PostPagedResponse response = mapper.toPostPagedResponse(posts, likedStatuses);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get post by ID", description = "Get detailed information about a specific post")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        MarketplacePost post = postService.getPostById(id);
        
        // Increment view count
        postService.incrementViewCount(id);
        
        // Reload post to get updated view count
        post = postService.getPostById(id);
        
        // Send realtime stats update
        notificationService.notifyPostStatsUpdate(
                id, 
                post.getLikeCount(), 
                post.getCommentCount(), 
                post.getViewCount()
        );
        
        // Check if liked (public access - anyone can view, but only authenticated users see their like status)
        boolean isLiked = false;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            String accessToken = principal.token();
            
            // Get residentId from userId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId != null) {
                isLiked = likeService.isLikedByUser(id, residentId);
            }
        }

        PostResponse response = mapper.toPostResponse(post, isLiked);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Create new post", description = "Create a new marketplace post")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestPart("data") CreatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Cannot find residentId for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Rate limiting
        if (!rateLimitService.canCreatePost(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }

        // Validate images
        if (images != null && !images.isEmpty()) {
            if (images.size() > 10) {
                return ResponseEntity.badRequest().build();
            }
            for (MultipartFile image : images) {
                try {
                    imageProcessingService.validateImage(image);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
        }

        // Create post
        MarketplacePost post = MarketplacePost.builder()
                .residentId(residentId)
                .buildingId(request.getBuildingId())
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .status(PostStatus.ACTIVE)
                .location(request.getLocation())
                .build();

        // Handle contact info - serialize to JSON string
        if (request.getContactInfo() != null) {
            try {
                String contactInfoJson = objectMapper.writeValueAsString(request.getContactInfo());
                post.setContactInfo(contactInfoJson);
            } catch (Exception e) {
                log.error("Error serializing contact info: {}", e.getMessage());
                post.setContactInfo("{}");
            }
        }

        MarketplacePost saved = postService.createPost(post);

        // Upload images asynchronously
        // Copy bytes before async processing to avoid file deletion issues
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                try {
                    // Copy bytes before async processing
                    byte[] imageBytes = image.getBytes();
                    String baseFileName = UUID.randomUUID().toString() + ".jpg";
                    asyncImageProcessingService.processImageAsync(
                            imageBytes, 
                            image.getOriginalFilename(), 
                            image.getContentType(),
                            saved.getId(), 
                            baseFileName,
                            i); // Sort order
                } catch (Exception e) {
                    log.error("Error copying image bytes for async processing: {}", e.getMessage());
                }
            }
        }

        // Notify new post
        notificationService.notifyNewPost(request.getBuildingId(), saved.getId(), saved.getTitle());
        
        // Send initial stats
        notificationService.notifyPostStatsUpdate(
                saved.getId(), 
                saved.getLikeCount(), 
                saved.getCommentCount(), 
                saved.getViewCount()
        );

        PostResponse response = mapper.toPostResponse(saved, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update post", description = "Update an existing post")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestPart("data") UpdatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Update fields
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getPrice() != null) post.setPrice(request.getPrice());
        if (request.getCategory() != null) post.setCategory(request.getCategory());
        if (request.getLocation() != null) post.setLocation(request.getLocation());

        MarketplacePost updated = postService.updatePost(id, post);

        // Handle image deletion
        if (request.getImagesToDelete() != null && !request.getImagesToDelete().isEmpty()) {
            for (String imageIdStr : request.getImagesToDelete()) {
                try {
                    UUID imageId = UUID.fromString(imageIdStr);
                    // Find image in database
                    var imageOpt = imageRepository.findById(imageId);
                    if (imageOpt.isPresent()) {
                        MarketplacePostImage image = imageOpt.get();
                        // Verify image belongs to this post
                        if (image.getPost().getId().equals(id)) {
                            // Delete from database
                            imageRepository.delete(image);
                            log.info("Deleted image from database: {}", imageId);
                            
                            // Delete from storage
                            // Extract filename from imageUrl (format: /api/marketplace/uploads/{postId}/{fileName})
                            String imageUrl = image.getImageUrl();
                            if (imageUrl != null && imageUrl.contains("/uploads/")) {
                                String[] parts = imageUrl.split("/uploads/");
                                if (parts.length > 1) {
                                    String filePath = parts[1]; // {postId}/{fileName}
                                    String[] pathParts = filePath.split("/");
                                    if (pathParts.length > 1) {
                                        String fileName = pathParts[1];
                                        fileStorageService.deleteImage(id.toString(), fileName);
                                        log.info("Deleted image file from storage: {}", fileName);
                                    }
                                }
                            }
                            
                            // Also delete thumbnail if exists
                            String thumbnailUrl = image.getThumbnailUrl();
                            if (thumbnailUrl != null && thumbnailUrl.contains("/uploads/")) {
                                String[] parts = thumbnailUrl.split("/uploads/");
                                if (parts.length > 1) {
                                    String filePath = parts[1];
                                    String[] pathParts = filePath.split("/");
                                    if (pathParts.length > 1) {
                                        String fileName = pathParts[1];
                                        fileStorageService.deleteImage(id.toString(), fileName);
                                        log.info("Deleted thumbnail file from storage: {}", fileName);
                                    }
                                }
                            }
                        } else {
                            log.warn("Image {} does not belong to post {}", imageId, id);
                        }
                    } else {
                        log.warn("Image not found in database: {}", imageId);
                    }
                } catch (Exception e) {
                    log.error("Error deleting image {}: {}", imageIdStr, e.getMessage(), e);
                }
            }
        }

        // Handle new images
        // Copy bytes before async processing to avoid file deletion issues
        if (newImages != null && !newImages.isEmpty()) {
            // Get current image count to determine sort order
            int currentImageCount = updated.getImages().size();
            for (int i = 0; i < newImages.size(); i++) {
                MultipartFile image = newImages.get(i);
                try {
                    // Copy bytes before async processing
                    byte[] imageBytes = image.getBytes();
                    String baseFileName = UUID.randomUUID().toString() + ".jpg";
                    asyncImageProcessingService.processImageAsync(
                            imageBytes, 
                            image.getOriginalFilename(), 
                            image.getContentType(),
                            updated.getId(), 
                            baseFileName,
                            currentImageCount + i); // Sort order
                } catch (Exception e) {
                    log.error("Error copying image bytes for async processing: {}", e.getMessage());
                }
            }
        }

        PostResponse response = mapper.toPostResponse(updated, false);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete post", description = "Delete a post")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(id);
        fileStorageService.deletePostImages(id.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update post status", description = "Update post status (e.g., ACTIVE -> SOLD)")
    public ResponseEntity<PostResponse> updatePostStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PostStatus status = PostStatus.valueOf(request.getStatus().toUpperCase());
        MarketplacePost updated = postService.updatePostStatus(id, status);

        // Notify status change
        // notificationService.notifyPostStatusChange(id, status.name());

        PostResponse response = mapper.toPostResponse(updated, false);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my posts", description = "Get posts created by the authenticated user")
    public ResponseEntity<PostPagedResponse> getMyPosts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PostStatus postStatus = status != null ? PostStatus.valueOf(status.toUpperCase()) : null;
        Page<MarketplacePost> posts = postService.getMyPosts(residentId, postStatus, page, size);

        List<Boolean> likedStatuses = posts.getContent().stream()
                .map(post -> likeService.isLikedByUser(post.getId(), userId))
                .collect(Collectors.toList());

        PostPagedResponse response = mapper.toPostPagedResponse(posts, likedStatuses);
        return ResponseEntity.ok(response);
    }
}

