package com.QhomeBase.marketplaceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for real-time notifications via WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify new comment on a post
     */
    public void notifyNewComment(UUID postId, UUID commentId, UUID authorId, String authorName) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_COMMENT");
        message.put("postId", postId.toString());
        message.put("commentId", commentId.toString());
        message.put("authorId", authorId.toString());
        message.put("authorName", authorName);
        message.put("timestamp", System.currentTimeMillis());

        // Send to post owner and all users viewing the post
        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/comments", message);
        log.info("Sent new comment notification for post: {}", postId);
    }

    /**
     * Notify new like on a post
     */
    public void notifyNewLike(UUID postId, UUID userId, String userName, boolean isLiked) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", isLiked ? "POST_LIKED" : "POST_UNLIKED");
        message.put("postId", postId.toString());
        message.put("userId", userId.toString());
        message.put("userName", userName);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/likes", message);
        log.info("Sent like notification for post: {} (liked: {})", postId, isLiked);
    }

    /**
     * Notify new post in building
     */
    public void notifyNewPost(UUID buildingId, UUID postId, String title) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_POST");
        message.put("postId", postId.toString());
        message.put("title", title);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/building/" + buildingId + "/posts", message);
        log.info("Sent new post notification for building: {}", buildingId);
    }

    /**
     * Notify post status change
     */
    public void notifyPostStatusChange(UUID postId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "POST_STATUS_CHANGED");
        message.put("postId", postId.toString());
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/status", message);
        log.info("Sent post status change notification: {} -> {}", postId, status);
    }

    /**
     * Notify post stats update (like count, comment count, view count)
     */
    public void notifyPostStatsUpdate(UUID postId, Long likeCount, Long commentCount, Long viewCount) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "POST_STATS_UPDATE");
        message.put("postId", postId.toString());
        message.put("likeCount", likeCount);
        message.put("commentCount", commentCount);
        message.put("viewCount", viewCount);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/stats", message);
        log.debug("Sent post stats update for post: {} (likes: {}, comments: {}, views: {})", 
                postId, likeCount, commentCount, viewCount);
    }
}

