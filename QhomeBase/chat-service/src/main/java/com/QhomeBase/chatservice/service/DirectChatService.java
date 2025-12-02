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

        // Check if blocked
        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        if (blockRepository.findBlocksBetweenUsers(residentId, otherParticipantId).size() > 0) {
            throw new RuntimeException("Cannot access conversation: User is blocked");
        }

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
        if (!"ACTIVE".equals(conversation.getStatus())) {
            throw new RuntimeException("Conversation is not active");
        }

        // Check if blocked
        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        if (blockRepository.findBlocksBetweenUsers(residentId, otherParticipantId).size() > 0) {
            throw new RuntimeException("Cannot send message: User is blocked");
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

        message = messageRepository.save(message);
        messageRepository.flush(); // Ensure createdAt is set

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

        // Send FCM push notification to other participant
        fcmPushService.sendDirectMessageNotification(otherParticipantId, conversationId, response);

        return response;
    }

    /**
     * Get messages in a conversation with pagination
     */
    @Transactional(readOnly = true)
    public DirectMessagePagedResponse getMessages(UUID conversationId, UUID userId, int page, int size) {
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

        Pageable pageable = PageRequest.of(page, size);
        Page<DirectMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        // Update last read time
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (participant != null) {
            participant.setLastReadAt(OffsetDateTime.now());
            participantRepository.save(participant);
        }

        return DirectMessagePagedResponse.builder()
                .content(messages.getContent().stream()
                        .map(msg -> toDirectMessageResponse(msg, accessToken))
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

    private ConversationResponse toConversationResponse(Conversation conversation, UUID currentUserId, String accessToken) {
        UUID otherParticipantId = conversation.getOtherParticipantId(currentUserId);
        String otherParticipantName = residentInfoService.getResidentName(otherParticipantId, accessToken);
        String participant1Name = residentInfoService.getResidentName(conversation.getParticipant1Id(), accessToken);
        String participant2Name = residentInfoService.getResidentName(conversation.getParticipant2Id(), accessToken);

        // Get last message
        DirectMessage lastMessage = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);

        // Get unread count
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
            unreadCount = messageRepository.countUnreadMessages(conversation.getId(), currentUserId, lastReadAt);
        }

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
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage message, String accessToken) {
        String senderName = message.getSenderId() != null 
            ? residentInfoService.getResidentName(message.getSenderId(), accessToken)
            : "System";

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
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt());

        // Load reply message if exists
        if (message.getReplyToMessageId() != null) {
            messageRepository.findById(message.getReplyToMessageId())
                    .ifPresent(replyMessage -> builder.replyToMessage(toDirectMessageResponse(replyMessage, accessToken)));
        }

        return builder.build();
    }
}

