package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostImageRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplacePostService {

    private final MarketplacePostRepository postRepository;
    private final MarketplacePostImageRepository imageRepository;
    private final CacheService cacheService;

    /**
     * Get post by ID - cached for 5 minutes
     */
    @Cacheable(value = "postDetails", key = "#postId")
    @Transactional(readOnly = true)
    public MarketplacePost getPostById(UUID postId) {
        log.debug("Fetching post from database: {}", postId);
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
    }

    /**
     * Get posts with filters - cached for 2 minutes
     */
    @Cacheable(value = "postList", key = "#buildingId + '_' + #status + '_' + (#category != null ? #category : 'all') + '_' + #page + '_' + #size + '_' + (#sortBy != null ? #sortBy : 'newest')")
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getPosts(
            UUID buildingId,
            PostStatus status,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            String sortBy,
            int page,
            int size) {
        
        log.debug("Fetching posts from database: buildingId={}, status={}, category={}, page={}, size={}", 
                buildingId, status, category, page, size);

        // Default sort
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "newest";
        }

        Pageable pageable = PageRequest.of(page, size);
        
        Page<MarketplacePost> posts = postRepository.findPostsWithFilters(
                buildingId, status.name(), category, minPrice, maxPrice, search, sortBy, pageable
        );
        
        // Load images for all posts in batch to avoid N+1 problem and LazyInitializationException
        if (!posts.getContent().isEmpty()) {
            List<UUID> postIds = posts.getContent().stream()
                    .map(MarketplacePost::getId)
                    .toList();
            
            // Load all images for these posts in one query
            List<MarketplacePostImage> allImages = imageRepository.findByPostIdIn(postIds);
            
            // Group images by post ID and set them to posts
            java.util.Map<UUID, List<MarketplacePostImage>> imagesByPostId = allImages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(img -> img.getPost().getId()));
            
            // Set images to each post
            posts.getContent().forEach(post -> {
                List<MarketplacePostImage> postImages = imagesByPostId.getOrDefault(
                        post.getId(), 
                        new java.util.ArrayList<>()
                );
                post.setImages(postImages);
            });
        }
        
        return posts;
    }

    /**
     * Get popular posts - cached for 15 minutes
     */
    @Cacheable(value = "popularPosts", key = "#buildingId + '_' + #page")
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getPopularPosts(UUID buildingId, int page, int size) {
        log.debug("Fetching popular posts from database: buildingId={}, page={}", buildingId, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likeCount", "createdAt"));
        return postRepository.findByBuildingIdAndStatusOrderByCreatedAtDesc(buildingId, PostStatus.ACTIVE, pageable);
    }

    /**
     * Get my posts
     */
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getMyPosts(UUID residentId, PostStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return postRepository.findByResidentIdAndStatusOrderByCreatedAtDesc(residentId, status, pageable);
        }
        return postRepository.findByResidentIdOrderByCreatedAtDesc(residentId, pageable);
    }

    /**
     * Create post - evicts cache
     */
    @CacheEvict(value = {"postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost createPost(MarketplacePost post) {
        log.info("Creating new post: {}", post.getTitle());
        MarketplacePost saved = postRepository.save(post);
        // Also evict post details cache for this post
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Update post - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost updatePost(UUID postId, MarketplacePost post) {
        log.info("Updating post: {}", postId);
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found: " + postId);
        }
        post.setId(postId);
        MarketplacePost saved = postRepository.save(post);
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Delete post - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public void deletePost(UUID postId) {
        log.info("Deleting post: {}", postId);
        postRepository.deleteById(postId);
        cacheService.evictPostCaches(postId);
    }

    /**
     * Update post status - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost updatePostStatus(UUID postId, PostStatus status) {
        log.info("Updating post status: {} -> {}", postId, status);
        MarketplacePost post = getPostById(postId);
        post.setStatus(status);
        MarketplacePost saved = postRepository.save(post);
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Increment view count
     */
    @Transactional
    public void incrementViewCount(UUID postId) {
        MarketplacePost post = getPostById(postId);
        post.incrementViewCount();
        postRepository.save(post);
        // Evict cache to reflect new view count
        cacheService.evictCacheEntry("postDetails", postId);
    }
}

