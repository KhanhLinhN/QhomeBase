package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.model.*;
import com.QhomeBase.chatservice.repository.*;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectChatService {

    private final DirectMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectChatFileRepository fileRepository;
    private final BlockRepository blockRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final FriendshipService friendshipService;
    private final DirectMessageDeletionRepository messageDeletionRepository;

    private String getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.token();
        }
        return null;
    }

    /**
     * Get or create conversation between two users
     */
    @Transactional(readOnly = true)
    public Conversation getOrCreateConversation(UUID userId1, UUID userId2) {
        // Ensure participant1_id < participant2_id for uniqueness
        UUID participant1Id = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID participant2Id = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id)
                .orElse(null);
    }

    /**
     * Get all active conversations for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<Conversation> conversations = conversationRepository
                .findActiveConversationsByUserId(residentId);

        return conversations.stream()
                .filter(conv -> {
                    // Filter out DELETED conversations (both participants have hidden)
                    if ("DELETED".equals(conv.getStatus())) {
                        return false;
                    }
                    // Filter out conversations hidden by current user
                    ConversationParticipant participant = participantRepository
                            .findByConversationIdAndResidentId(conv.getId(), residentId)
                            .orElse(null);
                    return participant == null || !Boolean.TRUE.equals(participant.getIsHidden());
                })
                .map(conv -> toConversationResponse(conv, residentId, accessToken))
                .collect(Collectors.toList());
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check if conversation is DELETED (both participants have hidden it)
        if ("DELETED".equals(conversation.getStatus())) {
            throw new RuntimeException("Conversation has been deleted by both participants.");
        }

        // Check if conversation is hidden for this user
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
            throw new RuntimeException("Conversation is hidden. You cannot view this conversation.");
        }

        // Don't block access to conversation - allow user to see messages even if blocked
        // Frontend will handle showing "User not found" message in input area if blocked

        return toConversationResponse(conversation, residentId, accessToken);
    }

    /**
     * Create a direct message
     */
    @Transactional
    public DirectMessageResponse createMessage(
            UUID conversationId,
            CreateDirectMessageRequest request,
            UUID userId) {
        
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check if conversation is active
        log.info("=== sendMessage ===");
        log.info("Conversation ID: {}", conversationId);
        log.info("Conversation status: {}", conversation.getStatus());
        log.info("Resident ID: {}", residentId);
        
        if (!"ACTIVE".equals(conversation.getStatus())) {
            log.error("Conversation is not active. Status: {}", conversation.getStatus());
            throw new RuntimeException("Conversation is not active. Current status: " + conversation.getStatus());
        }

        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        
        // Check if recipient has blocked sender - if so, sender cannot send messages
        // If A blocks B, then B cannot send messages to A (B's input field will show "người dùng hiện không tìm thấy")
        boolean recipientBlockedSender = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, residentId).isPresent();
        if (recipientBlockedSender) {
            log.warn("Recipient {} has blocked sender {}. Sender cannot send messages.", otherParticipantId, residentId);
            throw new RuntimeException("Người dùng hiện không tìm thấy");
        }
        
        // Check if sender has blocked recipient - sender can still send messages
        // If A blocks B, A can still send messages to B, but B won't see them (filtered in getMessages)
        Optional<Block> senderBlockedRecipient = blockRepository.findByBlockerIdAndBlockedId(residentId, otherParticipantId);
        if (senderBlockedRecipient.isPresent()) {
            log.info("Sender {} has blocked recipient {}. Message will be sent but recipient won't see it (filtered in getMessages).", residentId, otherParticipantId);
        }

        // Validate message type and content
        String messageType = request.getMessageType() != null ? request.getMessageType() : "TEXT";
        if ("TEXT".equals(messageType) && (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new RuntimeException("Text message content cannot be empty");
        }
        if ("IMAGE".equals(messageType) && (request.getImageUrl() == null || request.getImageUrl().isEmpty())) {
            throw new RuntimeException("Image message must have imageUrl");
        }
        if ("AUDIO".equals(messageType) && (request.getFileUrl() == null || request.getFileUrl().isEmpty())) {
            throw new RuntimeException("Audio message must have fileUrl");
        }
        if ("FILE".equals(messageType) && (request.getFileUrl() == null || request.getFileUrl().isEmpty())) {
            throw new RuntimeException("File message must have fileUrl");
        }
        if ("MARKETPLACE_POST".equals(messageType)) {
            if (request.getPostId() == null || request.getPostId().isEmpty()) {
                throw new RuntimeException("Marketplace post message must have postId");
            }
            if (request.getPostTitle() == null || request.getPostTitle().isEmpty()) {
                throw new RuntimeException("Marketplace post message must have postTitle");
            }
            // Store marketplace post data as JSON in content field
            String marketplaceData = String.format(
                "{\"postId\":\"%s\",\"postTitle\":\"%s\",\"postThumbnailUrl\":\"%s\",\"postPrice\":%s,\"deepLink\":\"%s\"}",
                request.getPostId() != null ? request.getPostId().replace("\"", "\\\"") : "",
                request.getPostTitle() != null ? request.getPostTitle().replace("\"", "\\\"") : "",
                request.getPostThumbnailUrl() != null ? request.getPostThumbnailUrl().replace("\"", "\\\"") : "",
                request.getPostPrice() != null ? request.getPostPrice() : "null",
                request.getDeepLink() != null ? request.getDeepLink().replace("\"", "\\\"") : ""
            );
            request.setContent(marketplaceData);
            // Use thumbnail as imageUrl for preview
            if (request.getPostThumbnailUrl() != null && !request.getPostThumbnailUrl().isEmpty()) {
                request.setImageUrl(request.getPostThumbnailUrl());
            }
        }

        // Create message - @CreationTimestamp will automatically set createdAt when persisted
        DirectMessage message = DirectMessage.builder()
                .conversation(conversation)
                .conversationId(conversationId)
                .senderId(residentId)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        // Save message - @CreationTimestamp will set createdAt to current server time
        OffsetDateTime beforeSave = OffsetDateTime.now();
        message = messageRepository.save(message);
        messageRepository.flush(); // Ensure timestamp is persisted immediately
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        // Log timestamp for debugging - verify it's set correctly
        OffsetDateTime messageCreatedAt = message.getCreatedAt();
        if (messageCreatedAt != null) {
            log.info("📅 Message timestamp - Created at: {} (offset: {}), Before save: {}, After save: {}", 
                    messageCreatedAt,
                    messageCreatedAt.getOffset(),
                    beforeSave,
                    afterSave);
        } else {
            log.error("⚠️ Message createdAt is null after save! This should not happen with @CreationTimestamp");
        }

        // Update conversation updated_at
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        // Update sender's lastReadAt to mark message as read
        ConversationParticipant senderParticipant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (senderParticipant != null) {
            OffsetDateTime createdAt = message.getCreatedAt();
            if (createdAt == null) {
                createdAt = OffsetDateTime.now();
            }
            senderParticipant.setLastReadAt(createdAt.plusNanos(1_000_000));
            participantRepository.save(senderParticipant);
        }

        DirectMessageResponse response = toDirectMessageResponse(message, accessToken);

        // Save file metadata if this is a file/image/audio message
        if ("FILE".equals(messageType) || "IMAGE".equals(messageType) || "AUDIO".equals(messageType)) {
            try {
                saveDirectChatFileMetadata(message);
            } catch (Exception e) {
                log.warn("Failed to save file metadata for message {}: {}", message.getId(), e.getMessage());
            }
        }

        // Notify via WebSocket
        notificationService.notifyDirectMessage(conversationId, response);

        // Unhide conversation if it was hidden (when new message arrives)
        ConversationParticipant otherParticipant = participantRepository
                .findByConversationIdAndResidentId(conversationId, otherParticipantId)
                .orElse(null);
        if (otherParticipant != null && Boolean.TRUE.equals(otherParticipant.getIsHidden())) {
            otherParticipant.setIsHidden(false);
            otherParticipant.setHiddenAt(null);
            // Reset lastReadAt to null so the new message is considered as the first message
            otherParticipant.setLastReadAt(null);
            participantRepository.save(otherParticipant);
            log.info("Conversation {} unhidden for resident {} (new message received). lastReadAt reset to null.", conversationId, otherParticipantId);
        }
        
        // If conversation was DELETED (both participants had hidden it), reset to ACTIVE
        if ("DELETED".equals(conversation.getStatus())) {
            conversation.setStatus("ACTIVE");
            conversationRepository.save(conversation);
            log.info("Conversation {} reset from DELETED to ACTIVE (new message received)", conversationId);
        }

        // Send FCM push notification to other participant
        fcmPushService.sendDirectMessageNotification(otherParticipantId, conversationId, response, residentId);

        return response;
    }

    /**
     * Get messages in a conversation with pagination
     */
    @Transactional
    public DirectMessagePagedResponse getMessages(UUID conversationId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        log.info("📥 [DirectChatService] getMessages called - conversationId: {}, userId: {}, residentId: {}, page: {}, size: {}", 
                conversationId, userId, residentId, page, size);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check if conversation is DELETED (both participants have hidden it)
        if ("DELETED".equals(conversation.getStatus())) {
            throw new RuntimeException("Conversation has been deleted by both participants.");
        }

        // Check if conversation is hidden for this user
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
            throw new RuntimeException("Conversation is hidden. You cannot view messages.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<DirectMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        // Get all deletion records for these messages
        List<UUID> messageIds = messages.getContent().stream()
                .map(DirectMessage::getId)
                .collect(Collectors.toList());
        
        List<DirectMessageDeletion> deletions = messageDeletionRepository.findByMessageIdIn(messageIds);
        
        // Get other participant ID
        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        
        // Check bidirectional blocking:
        // 1. If other participant (sender) has blocked current user (recipient)
        //    → Current user should not see messages from sender sent after block time
        // 2. If current user has blocked other participant (sender)
        //    → Current user should not see messages from sender sent after block time
        Optional<Block> senderBlockedRecipient = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, residentId);
        Optional<Block> recipientBlockedSender = blockRepository.findByBlockerIdAndBlockedId(residentId, otherParticipantId);
        
        final OffsetDateTime blockTime;
        if (senderBlockedRecipient.isPresent()) {
            blockTime = senderBlockedRecipient.get().getCreatedAt();
            log.debug("Sender {} has blocked recipient {} at {}", otherParticipantId, residentId, blockTime);
        } else if (recipientBlockedSender.isPresent()) {
            blockTime = recipientBlockedSender.get().getCreatedAt();
            log.debug("Recipient {} has blocked sender {} at {}", residentId, otherParticipantId, blockTime);
        } else {
            blockTime = null;
        }
        
        // Filter messages: if either party has blocked the other, hide messages sent after block time
        List<DirectMessage> allMessages = messages.getContent().stream()
                .filter(msg -> {
                    // Only filter messages from the other participant (sender)
                    if (msg.getSenderId() != null && msg.getSenderId().equals(otherParticipantId)) {
                        // If either party has blocked the other, filter out messages sent after block time
                        if (blockTime != null && msg.getCreatedAt() != null && msg.getCreatedAt().isAfter(blockTime)) {
                            log.debug("Filtering message {} - block relationship exists, block time: {}, message sent at {}", 
                                    msg.getId(), blockTime, msg.getCreatedAt());
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Update last read time (participant already loaded above)
        if (participant != null) {
            OffsetDateTime oldLastReadAt = participant.getLastReadAt();
            OffsetDateTime newLastReadAt = OffsetDateTime.now();
            participant.setLastReadAt(newLastReadAt);
            participantRepository.save(participant);
            log.info("✅ [DirectChatService] Updated lastReadAt for participant - conversationId: {}, residentId: {}, oldLastReadAt: {}, newLastReadAt: {}", 
                    conversationId, residentId, oldLastReadAt, newLastReadAt);
        } else {
            log.warn("⚠️ [DirectChatService] Participant is null, cannot update lastReadAt - conversationId: {}, residentId: {}", 
                    conversationId, residentId);
        }

        log.info("📤 [DirectChatService] Returning {} messages for conversationId: {}", 
                allMessages.size(), conversationId);

        // Create a map of messageId -> deletion for quick lookup
        Map<UUID, DirectMessageDeletion> deletionMap = deletions.stream()
                .filter(d -> d.getDeletedByUserId().equals(residentId) || 
                            d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE)
                .collect(Collectors.toMap(
                    DirectMessageDeletion::getMessageId,
                    d -> d,
                    (d1, d2) -> {
                        // If both FOR_ME and FOR_EVERYONE exist, prefer FOR_EVERYONE
                        if (d1.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE) {
                            return d1;
                        }
                        return d2;
                    }
                ));

        // Create a custom page response with all messages (including deleted ones)
        return DirectMessagePagedResponse.builder()
                .content(allMessages.stream()
                        .map(msg -> {
                            DirectMessageDeletion deletion = deletionMap.get(msg.getId());
                            return toDirectMessageResponse(msg, accessToken, deletion);
                        })
                        .collect(Collectors.toList()))
                .currentPage(messages.getNumber())
                .pageSize(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .first(messages.isFirst())
                .last(messages.isLast())
                .build();
    }

    /**
     * Delete a message
     * @param deleteType "FOR_ME" - only delete for current user, "FOR_EVERYONE" - delete for everyone
     * Only the sender can delete their own message
     */
    @Transactional
    public void deleteMessage(UUID conversationId, UUID messageId, UUID userId, String deleteType) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Validate deleteType
        DirectMessageDeletion.DeleteType type;
        try {
            type = DirectMessageDeletion.DeleteType.valueOf(deleteType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid deleteType. Must be FOR_ME or FOR_EVERYONE");
        }

        // Check conversation exists and user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Find message
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify message belongs to this conversation
        if (!message.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to this conversation");
        }

        // Only sender can delete their own message
        if (message.getSenderId() == null || !message.getSenderId().equals(residentId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Check if already deleted with same type
        List<DirectMessageDeletion> existingDeletions = messageDeletionRepository
                .findByMessageIdAndDeletedByUserId(messageId, residentId);
        boolean alreadyDeletedForMe = existingDeletions.stream()
                .anyMatch(d -> d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_ME);
        boolean alreadyDeletedForEveryone = existingDeletions.stream()
                .anyMatch(d -> d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE);

        if (type == DirectMessageDeletion.DeleteType.FOR_EVERYONE && alreadyDeletedForEveryone) {
            log.warn("Message {} already deleted for everyone", messageId);
            return;
        }
        if (type == DirectMessageDeletion.DeleteType.FOR_ME && alreadyDeletedForMe) {
            log.warn("Message {} already deleted for user {}", messageId, residentId);
            return;
        }

        // Create deletion record
        DirectMessageDeletion deletion = DirectMessageDeletion.builder()
                .messageId(messageId)
                .deletedByUserId(residentId)
                .deleteType(type)
                .deletedAt(OffsetDateTime.now())
                .build();
        messageDeletionRepository.save(deletion);

        // If deleting for everyone, also mark the message as deleted (legacy support)
        if (type == DirectMessageDeletion.DeleteType.FOR_EVERYONE) {
            message.setIsDeleted(true);
            messageRepository.save(message);
        }

        log.info("Message {} deleted by user {} with type {}", messageId, residentId, type);

        // Notify via WebSocket
        DirectMessageResponse response = toDirectMessageResponse(message, accessToken);
        notificationService.notifyDirectMessageDeleted(conversationId, messageId, response);
    }

    /**
     * Count unread messages in a conversation
     */
    @Transactional(readOnly = true)
    public Long countUnreadMessages(UUID conversationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            return 0L;
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);

        if (participant == null) {
            return 0L;
        }

        OffsetDateTime lastReadAt = participant.getLastReadAt();
        if (lastReadAt == null) {
            // If never read, count all messages except system messages
            lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        }

        return messageRepository.countUnreadMessages(conversationId, residentId, lastReadAt);
    }

    /**
     * Save file metadata for direct chat
     */
    @Transactional
    public DirectChatFile saveDirectChatFileMetadata(DirectMessage message) {
        if (!"FILE".equals(message.getMessageType()) && 
            !"IMAGE".equals(message.getMessageType()) && 
            !"AUDIO".equals(message.getMessageType())) {
            return null;
        }

        // Determine file URL and type
        String fileUrl = null;
        String fileType = null;
        if ("IMAGE".equals(message.getMessageType())) {
            fileUrl = message.getImageUrl();
            fileType = "IMAGE";
        } else if ("FILE".equals(message.getMessageType()) || "AUDIO".equals(message.getMessageType())) {
            fileUrl = message.getFileUrl();
            fileType = "AUDIO".equals(message.getMessageType()) ? "AUDIO" : "DOCUMENT";
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // Infer mimeType if not provided
        String mimeType = message.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = inferMimeType(message.getFileName(), fileType);
        }

        DirectChatFile file = DirectChatFile.builder()
                .conversation(message.getConversation())
                .conversationId(message.getConversationId())
                .message(message)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .fileName(message.getFileName() != null ? message.getFileName() : "file")
                .fileSize(message.getFileSize() != null ? message.getFileSize() : 0L)
                .fileType(fileType)
                .mimeType(mimeType)
                .fileUrl(fileUrl)
                .build();

        return fileRepository.save(file);
    }

    private String inferMimeType(String fileName, String fileType) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) return "application/msword";
        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lowerName.endsWith(".zip")) return "application/zip";
        if (lowerName.endsWith(".mp3")) return "audio/mpeg";
        if (lowerName.endsWith(".mp4")) return "video/mp4";
        
        return "application/octet-stream";
    }

    /**
     * Get all friends for current user
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<com.QhomeBase.chatservice.model.Friendship> friendships = friendshipService.getActiveFriendships(residentId);

        return friendships.stream()
                .map(friendship -> {
                    UUID friendId = friendship.getOtherUserId(residentId);
                    Map<String, Object> friendInfo = residentInfoService.getResidentInfo(friendId);
                    
                    // Get friend name - try multiple fields like other services do
                    String friendName = "Unknown";
                    if (friendInfo != null) {
                        Object nameObj = friendInfo.get("fullName");
                        if (nameObj != null) {
                            friendName = nameObj.toString();
                        } else {
                            nameObj = friendInfo.get("name");
                            if (nameObj != null) {
                                friendName = nameObj.toString();
                            } else {
                                // Try firstName + lastName
                                Object firstNameObj = friendInfo.get("firstName");
                                Object lastNameObj = friendInfo.get("lastName");
                                if (firstNameObj != null || lastNameObj != null) {
                                    String firstName = firstNameObj != null ? firstNameObj.toString() : "";
                                    String lastName = lastNameObj != null ? lastNameObj.toString() : "";
                                    friendName = (firstName + " " + lastName).trim();
                                    if (friendName.isEmpty()) {
                                        friendName = "Unknown";
                                    }
                                }
                            }
                        }
                    }
                    
                    String friendPhone = friendInfo != null ? (String) friendInfo.getOrDefault("phone", "") : "";

                    // Check if conversation exists
                    Conversation conversation = conversationRepository
                            .findConversationBetweenParticipants(residentId, friendId)
                            .orElse(null);

                    UUID conversationId = conversation != null ? conversation.getId() : null;
                    Boolean hasActiveConversation = conversation != null && "ACTIVE".equals(conversation.getStatus());

                    return FriendResponse.builder()
                            .friendId(friendId)
                            .friendName(friendName)
                            .friendPhone(friendPhone)
                            .conversationId(conversationId)
                            .hasActiveConversation(hasActiveConversation)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID currentUserId, String accessToken) {
        UUID otherParticipantId = conversation.getOtherParticipantId(currentUserId);
        String otherParticipantName = residentInfoService.getResidentName(otherParticipantId, accessToken);
        String participant1Name = residentInfoService.getResidentName(conversation.getParticipant1Id(), accessToken);
        String participant2Name = residentInfoService.getResidentName(conversation.getParticipant2Id(), accessToken);

        // Check bidirectional blocking to filter messages
        Optional<Block> senderBlockedRecipient = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, currentUserId);
        Optional<Block> recipientBlockedSender = blockRepository.findByBlockerIdAndBlockedId(currentUserId, otherParticipantId);
        
        final OffsetDateTime blockTime;
        if (senderBlockedRecipient.isPresent()) {
            blockTime = senderBlockedRecipient.get().getCreatedAt();
            log.debug("Sender {} has blocked recipient {} at {}", otherParticipantId, currentUserId, blockTime);
        } else if (recipientBlockedSender.isPresent()) {
            blockTime = recipientBlockedSender.get().getCreatedAt();
            log.debug("Recipient {} has blocked sender {} at {}", currentUserId, otherParticipantId, blockTime);
        } else {
            blockTime = null;
        }

        // Get last message - filter out blocked messages
        DirectMessage lastMessage = null;
        if (blockTime == null) {
            // No blocking, get last message normally
            lastMessage = messageRepository
                    .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 1))
                    .getContent()
                    .stream()
                    .findFirst()
                    .orElse(null);
        } else {
            // Block exists, need to find last non-blocked message
            // Get multiple messages and filter
            List<DirectMessage> messages = messageRepository
                    .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 50))
                    .getContent();
            
            lastMessage = messages.stream()
                    .filter(msg -> {
                        // Only filter messages from the other participant (sender)
                        if (msg.getSenderId() != null && msg.getSenderId().equals(otherParticipantId)) {
                            // If either party has blocked the other, filter out messages sent after block time
                            if (msg.getCreatedAt() != null && msg.getCreatedAt().isAfter(blockTime)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .findFirst()
                    .orElse(null);
            
            log.debug("Filtered last message - blockTime: {}, found: {}", blockTime, lastMessage != null ? lastMessage.getId() : "null");
        }

        // Get unread count - filter out blocked messages
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversation.getId(), currentUserId)
                .orElse(null);
        Long unreadCount = 0L;
        OffsetDateTime lastReadAt = null;
        if (participant != null) {
            lastReadAt = participant.getLastReadAt();
            if (lastReadAt == null) {
                lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
            }
            
            // Count unread messages, but exclude blocked messages
            if (blockTime == null) {
                // No blocking, count normally
                unreadCount = messageRepository.countUnreadMessages(conversation.getId(), currentUserId, lastReadAt);
            } else {
                // Block exists, need to count unread messages excluding blocked ones
                List<DirectMessage> unreadMessages = messageRepository
                        .findByConversationIdAndCreatedAtAfterOrderByCreatedAtDesc(conversation.getId(), lastReadAt, PageRequest.of(0, 1000))
                        .getContent();
                
                unreadCount = unreadMessages.stream()
                        .filter(msg -> {
                            // Only count messages from other participants (not from current user)
                            if (msg.getSenderId() == null || msg.getSenderId().equals(currentUserId)) {
                                return false;
                            }
                            
                            // Only filter messages from the other participant (sender)
                            if (msg.getSenderId().equals(otherParticipantId)) {
                                // If either party has blocked the other, filter out messages sent after block time
                                if (msg.getCreatedAt() != null && msg.getCreatedAt().isAfter(blockTime)) {
                                    return false;
                                }
                            }
                            
                            // Also exclude deleted messages and system messages
                            if (Boolean.TRUE.equals(msg.getIsDeleted()) || "SYSTEM".equals(msg.getMessageType())) {
                                return false;
                            }
                            
                            return true;
                        })
                        .count();
                
                log.debug("Filtered unread count - blockTime: {}, unreadCount: {}", blockTime, unreadCount);
            }
            
            log.info("📊 [DirectChatService] getConversation - conversationId: {}, currentUserId: {}, lastReadAt: {}, unreadCount: {}", 
                    conversation.getId(), currentUserId, lastReadAt, unreadCount);
        } else {
            log.warn("⚠️ [DirectChatService] getConversation - Participant not found for conversationId: {}, currentUserId: {}", 
                    conversation.getId(), currentUserId);
        }

        // Check if current user is blocked by the other participant
        boolean isBlockedByOther = senderBlockedRecipient.isPresent();

        return ConversationResponse.builder()
                .id(conversation.getId())
                .participant1Id(conversation.getParticipant1Id())
                .participant2Id(conversation.getParticipant2Id())
                .participant1Name(participant1Name)
                .participant2Name(participant2Name)
                .status(conversation.getStatus())
                .createdBy(conversation.getCreatedBy())
                .lastMessage(lastMessage != null ? toDirectMessageResponse(lastMessage, accessToken) : null)
                .unreadCount(unreadCount)
                .lastReadAt(lastReadAt)
                .isBlockedByOther(isBlockedByOther)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage message, String accessToken) {
        return toDirectMessageResponse(message, accessToken, null);
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage message, String accessToken, DirectMessageDeletion deletion) {
        String senderName = message.getSenderId() != null 
            ? residentInfoService.getResidentName(message.getSenderId(), accessToken)
            : "System";

        // Determine deleteType
        String deleteType = null;
        if (deletion != null) {
            deleteType = deletion.getDeleteType().name();
        } else if (Boolean.TRUE.equals(message.getIsDeleted())) {
            // Legacy support: if isDeleted is true but no deletion record, assume FOR_EVERYONE
            deleteType = "FOR_EVERYONE";
        }

        DirectMessageResponse.DirectMessageResponseBuilder builder = DirectMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .mimeType(message.getMimeType())
                .replyToMessageId(message.getReplyToMessageId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted() || deletion != null)
                .deleteType(deleteType)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt());
        
        // Parse marketplace_post data from content if messageType is MARKETPLACE_POST
        if ("MARKETPLACE_POST".equals(message.getMessageType()) && message.getContent() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> marketplaceData = objectMapper.readValue(message.getContent(), java.util.Map.class);
                builder.postId((String) marketplaceData.get("postId"));
                builder.postTitle((String) marketplaceData.get("postTitle"));
                builder.postThumbnailUrl((String) marketplaceData.get("postThumbnailUrl"));
                Object priceObj = marketplaceData.get("postPrice");
                if (priceObj != null && !"null".equals(priceObj.toString())) {
                    if (priceObj instanceof Number) {
                        builder.postPrice(((Number) priceObj).doubleValue());
                    } else {
                        builder.postPrice(Double.parseDouble(priceObj.toString()));
                    }
                }
                builder.deepLink((String) marketplaceData.get("deepLink"));
            } catch (Exception e) {
                log.warn("Failed to parse marketplace_post data: {}", e.getMessage());
            }
        }

        // Load reply message if exists
        if (message.getReplyToMessageId() != null) {
            messageRepository.findById(message.getReplyToMessageId())
                    .ifPresent(replyMessage -> builder.replyToMessage(toDirectMessageResponse(replyMessage, accessToken)));
        }

        return builder.build();
    }

    /**
     * Get files in a conversation with pagination
     */
    @Transactional(readOnly = true)
    public DirectChatFilePagedResponse getFiles(UUID conversationId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }
        
        // Verify user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        
        // Check if user is a participant using residentId
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DirectChatFile> filePage = fileRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, pageable);
        
        List<DirectChatFileResponse> fileResponses = filePage.getContent().stream()
                .map(file -> toFileResponse(file, accessToken))
                .collect(Collectors.toList());
        
        return DirectChatFilePagedResponse.builder()
                .content(fileResponses)
                .currentPage(filePage.getNumber())
                .pageSize(filePage.getSize())
                .totalElements(filePage.getTotalElements())
                .totalPages(filePage.getTotalPages())
                .hasNext(filePage.hasNext())
                .hasPrevious(filePage.hasPrevious())
                .first(filePage.isFirst())
                .last(filePage.isLast())
                .build();
    }
    
    /**
     * Convert DirectChatFile entity to DTO
     */
    private DirectChatFileResponse toFileResponse(DirectChatFile file, String accessToken) {
        String senderName = file.getSenderId() != null
            ? residentInfoService.getResidentName(file.getSenderId(), accessToken)
            : "Unknown";
        
        return DirectChatFileResponse.builder()
                .id(file.getId())
                .conversationId(file.getConversationId())
                .messageId(file.getMessageId())
                .senderId(file.getSenderId())
                .senderName(senderName)
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .fileUrl(file.getFileUrl())
                .createdAt(file.getCreatedAt())
                .build();
    }
}

