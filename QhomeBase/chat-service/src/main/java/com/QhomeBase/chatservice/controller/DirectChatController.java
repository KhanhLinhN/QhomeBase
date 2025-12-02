package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.BlockService;
import com.QhomeBase.chatservice.service.DirectChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct-chat")
@RequiredArgsConstructor
@Tag(name = "Direct Chat", description = "APIs for 1-1 direct chat")
public class DirectChatController {

    private final DirectChatService directChatService;
    private final BlockService blockService;

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

    @PostMapping("/block/{blockedId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Block user", description = "Block a user from sending/receiving messages")
    public ResponseEntity<Void> blockUser(
            @PathVariable UUID blockedId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Get residentId
        // TODO: Use ResidentInfoService to get residentId from userId
        blockService.blockUser(userId, blockedId);
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
        
        blockService.unblockUser(userId, blockedId);
        return ResponseEntity.ok().build();
    }
}

