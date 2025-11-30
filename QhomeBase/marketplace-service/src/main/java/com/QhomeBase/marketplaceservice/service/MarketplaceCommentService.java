package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.repository.MarketplaceCommentRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
     * Get comments for a post
     */
    @Transactional(readOnly = true)
    public List<MarketplaceComment> getComments(UUID postId) {
        List<MarketplaceComment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
        // Force initialization of replies collection within transaction
        comments.forEach(comment -> {
            if (comment.getReplies() != null) {
                comment.getReplies().size(); // Force initialization
                // Also initialize nested replies
                comment.getReplies().forEach(reply -> {
                    if (reply.getReplies() != null) {
                        reply.getReplies().size();
                    }
                });
            }
        });
        return comments;
    }

    /**
     * Add comment
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public MarketplaceComment addComment(UUID postId, UUID residentId, String content, UUID parentCommentId) {
        log.info("Adding comment to post: {} by user: {}", postId, residentId);

        MarketplacePost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        MarketplaceComment comment = MarketplaceComment.builder()
                .post(post)
                .residentId(residentId)
                .content(content)
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
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public void deleteComment(UUID commentId, UUID residentId) {
        log.info("Deleting comment: {} by user: {}", commentId, residentId);

        MarketplaceComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getResidentId().equals(residentId)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        comment.markAsDeleted();
        commentRepository.save(comment);

        // Decrement comment count
        MarketplacePost post = comment.getPost();
        post.decrementCommentCount();
        postRepository.save(post);
    }
}

