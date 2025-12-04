package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.BlockService;
import com.QhomeBase.chatservice.service.DirectChatService;
import com.QhomeBase.chatservice.service.ResidentInfoService;
import com.QhomeBase.chatservice.service.ConversationMuteService;
import com.QhomeBase.chatservice.service.ConversationHideService;
import com.QhomeBase.chatservice.dto.FriendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct-chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Direct Chat", description = "APIs for 1-1 direct chat")
public class DirectChatController {

    private final DirectChatService directChatService;
    private final BlockService blockService;
    private final ResidentInfoService residentInfoService;
    private final ConversationMuteService conversationMuteService;
    private final ConversationHideService conversationHideService;

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get all conversations", description = "Get all active conversations for current user")
    public ResponseEntity<List<ConversationResponse>> getConversations(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        List<ConversationResponse> conversations = directChatService.getConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get conversation", description = "Get conversation details by ID")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        ConversationResponse conversation = directChatService.getConversation(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Send message", description = "Send a message in a direct chat conversation")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody CreateDirectMessageRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectMessageResponse message = directChatService.createMessage(conversationId, request, userId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get messages", description = "Get messages in a conversation with pagination")
    public ResponseEntity<DirectMessagePagedResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectMessagePagedResponse messages = directChatService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}/unread-count")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get unread count", description = "Get unread message count in a conversation")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        Long unreadCount = directChatService.countUnreadMessages(conversationId, userId);
        return ResponseEntity.ok(unreadCount);
    }

    @GetMapping("/friends")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get friends list", description = "Get all active friends for current user")
    public ResponseEntity<List<FriendResponse>> getFriends(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        List<FriendResponse> friends = directChatService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/conversations/{conversationId}/files")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get files", description = "Get files in a conversation with pagination")
    public ResponseEntity<DirectChatFilePagedResponse> getFiles(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectChatFilePagedResponse files = directChatService.getFiles(conversationId, userId, page, size);
        return ResponseEntity.ok(files);
    }

    @PostMapping("/block/{blockedId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Block user", description = "Block a user from sending/receiving messages")
    public ResponseEntity<Void> blockUser(
            @PathVariable UUID blockedId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Convert userId to residentId
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(userId);
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(blockedId);
        
        blockService.blockUser(blockerResidentId, blockedResidentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{blockedId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Unblock user", description = "Unblock a user")
    public ResponseEntity<Void> unblockUser(
            @PathVariable UUID blockedId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Convert userId to residentId
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(userId);
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(blockedId);
        
        blockService.unblockUser(blockerResidentId, blockedResidentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/blocked-users")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get blocked users", description = "Get list of users blocked by current user")
    public ResponseEntity<List<UUID>> getBlockedUsers(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            
            // Convert userId to residentId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId);
            if (residentId == null) {
                // If residentId is null, return empty list instead of throwing error
                // This can happen if user doesn't have a resident record yet
                return ResponseEntity.ok(List.of());
            }
            
            // Get blocked residentIds and convert back to userIds
            List<UUID> blockedResidentIds = blockService.getBlockedUserIds(residentId);
            
            // Filter out null values when converting residentIds to userIds
            // Some residents might not have userIds yet
            List<UUID> blockedUserIds = blockedResidentIds.stream()
                    .map(residentInfoService::getUserIdFromResidentId)
                    .filter(java.util.Objects::nonNull) // Filter out null values
                    .toList();
            
            return ResponseEntity.ok(blockedUserIds);
        } catch (Exception e) {
            // Log error and return empty list instead of throwing 500
            // This prevents the app from crashing if there's an issue
            log.error("Error getting blocked users", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/is-blocked/{userId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Check if user is blocked", description = "Check if a user is blocked by current user")
    public ResponseEntity<Boolean> isBlocked(
            @PathVariable UUID userId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID blockerUserId = principal.uid();
        
        // Convert userIds to residentIds
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(blockerUserId);
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(userId);
        
        boolean isBlocked = blockService.isBlocked(blockerResidentId, blockedResidentId);
        return ResponseEntity.ok(isBlocked);
    }

    @PostMapping("/conversations/{conversationId}/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mute direct conversation", description = "Mute notifications for a direct conversation. durationHours: 1, 2, 24, or null (indefinitely)")
    public ResponseEntity<Map<String, Object>> muteDirectConversation(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Integer durationHours,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.muteDirectConversation(conversationId, userId, durationHours);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @DeleteMapping("/conversations/{conversationId}/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Unmute direct conversation", description = "Unmute notifications for a direct conversation")
    public ResponseEntity<Map<String, Object>> unmuteDirectConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.unmuteDirectConversation(conversationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/conversations/{conversationId}/hide")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Hide direct conversation", description = "Hide a direct conversation from chat list (client-side only). Resets unreadCount to 0.")
    public ResponseEntity<Map<String, Object>> hideDirectConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationHideService.hideDirectConversation(conversationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }
}

