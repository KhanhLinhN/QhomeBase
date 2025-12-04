package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.repository.MarketplaceCommentRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceCommentService {

    private final MarketplaceCommentRepository commentRepository;
    private final MarketplacePostRepository postRepository;
    private final MarketplaceNotificationService notificationService;

    /**
     * Get comments for a post (all comments, no pagination)
     */
    @Transactional(readOnly = true)
    public List<MarketplaceComment> getComments(UUID postId) {
        List<MarketplaceComment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
        // Force initialization of all nested replies recursively within transaction
        comments.forEach(this::initializeReplies);
        return comments;
    }

    /**
     * Get paginated comments for a post
     */
    @Transactional(readOnly = true)
    public Page<MarketplaceComment> getComments(UUID postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MarketplaceComment> commentsPage = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId, pageable);
        // Force initialization of all nested replies recursively within transaction
        commentsPage.getContent().forEach(this::initializeReplies);
        return commentsPage;
    }

    /**
     * Recursively initialize all nested replies to prevent LazyInitializationException
     * Also filters out deleted replies
     */
    private void initializeReplies(MarketplaceComment comment) {
        if (comment.getReplies() != null) {
            // Force initialization of the replies collection
            Hibernate.initialize(comment.getReplies());
            // Filter out deleted replies and recursively initialize remaining ones
            comment.getReplies().removeIf(MarketplaceComment::isDeleted);
            comment.getReplies().forEach(this::initializeReplies);
        }
    }

    /**
     * Add comment
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public MarketplaceComment addComment(UUID postId, UUID residentId, String content, UUID parentCommentId, String imageUrl, String videoUrl) {
        log.info("Adding comment to post: {} by user: {}", postId, residentId);

        MarketplacePost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        MarketplaceComment comment = MarketplaceComment.builder()
                .post(post)
                .residentId(residentId)
                .content(content)
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .build();

        if (parentCommentId != null) {
            MarketplaceComment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));
            comment.setParentComment(parent);
        }

        MarketplaceComment saved = commentRepository.save(comment);
        post.incrementCommentCount();
        postRepository.save(post);

        // Notify post owner and viewers
        notificationService.notifyNewComment(postId, saved.getId(), residentId, "User");

        return saved;
    }

    /**
     * Update comment
     */
    @Transactional
    public MarketplaceComment updateComment(UUID commentId, UUID residentId, String content) {
        log.info("Updating comment: {} by user: {}", commentId, residentId);

        MarketplaceComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getResidentId().equals(residentId)) {
            throw new RuntimeException("Not authorized to update this comment");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    /**
     * Delete comment (soft delete)
     * Rules:
     * - Post owner can delete any comment in their post
     * - Comment owner can only delete their own comment
     * - TH1: When deleting a root comment (parentComment is null), delete entire sub-tree (all levels recursively)
     * - TH2: When deleting a child comment (parentComment != null), delete only that comment, do NOT delete any replies
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public void deleteComment(UUID commentId, UUID residentId) {
        log.info("Deleting comment: {} by user: {}", commentId, residentId);

        MarketplaceComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        MarketplacePost post = comment.getPost();
        UUID postOwnerId = post.getResidentId();
        UUID commentOwnerId = comment.getResidentId();

        // Check authorization: post owner OR comment owner
        boolean isPostOwner = postOwnerId.equals(residentId);
        boolean isCommentOwner = commentOwnerId.equals(residentId);

        if (!isPostOwner && !isCommentOwner) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        // TH1: If deleting a root comment (parentComment is null), delete entire sub-tree recursively
        if (comment.getParentComment() == null) {
            // Delete all replies recursively (all levels)
            List<MarketplaceComment> directChildren = comment.getReplies();
            if (directChildren != null && !directChildren.isEmpty()) {
                log.info("TH1: Deleting entire sub-tree of root comment {} ({} direct children)", commentId, directChildren.size());
                int deletedCount = deleteRepliesRecursively(directChildren, post);
                log.info("TH1: Deleted {} comments in sub-tree recursively", deletedCount);
            }
        } else {
            // TH2: Deleting a child comment (parentComment != null)
            // Only delete this comment, do NOT delete any replies
            // Move replies to parent comment so they remain visible
            List<MarketplaceComment> childReplies = comment.getReplies();
            if (childReplies != null && !childReplies.isEmpty()) {
                MarketplaceComment parentComment = comment.getParentComment();
                log.info("TH2: Deleting child comment {} only, moving {} replies to parent comment {}", 
                        commentId, childReplies.size(), parentComment.getId());
                
                // Move replies to parent comment
                for (MarketplaceComment reply : childReplies) {
                    reply.setParentComment(parentComment);
                    commentRepository.save(reply);
                }
            }
            log.info("TH2: Deleted child comment {} only, kept all replies", commentId);
        }

        // Mark the comment itself as deleted
        comment.markAsDeleted();
        commentRepository.save(comment);

        // Decrement comment count
        post.decrementCommentCount();
        postRepository.save(post);
    }

    /**
     * Recursively delete all replies to a comment
     * @param replies List of replies to delete
     * @param post The post to decrement comment count
     * @return Number of comments deleted
     */
    private int deleteRepliesRecursively(List<MarketplaceComment> replies, MarketplacePost post) {
        int deletedCount = 0;
        for (MarketplaceComment reply : replies) {
            // Recursively delete replies of this reply
            List<MarketplaceComment> nestedReplies = reply.getReplies();
            if (nestedReplies != null && !nestedReplies.isEmpty()) {
                deletedCount += deleteRepliesRecursively(nestedReplies, post);
            }
            
            // Mark this reply as deleted
            reply.markAsDeleted();
            commentRepository.save(reply);
            post.decrementCommentCount();
            deletedCount++;
        }
        return deletedCount;
    }
}

