package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.CreateMessageRequest;
import com.QhomeBase.chatservice.dto.MessagePagedResponse;
import com.QhomeBase.chatservice.dto.MessageResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.model.Message;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.repository.MessageRepository;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;

    @Transactional
    public MessageResponse createMessage(UUID groupId, CreateMessageRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Validate message type and content
        String messageType = request.getMessageType() != null ? request.getMessageType() : "TEXT";
        if ("TEXT".equals(messageType) && (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new RuntimeException("Text message content cannot be empty");
        }
        if ("IMAGE".equals(messageType) && (request.getImageUrl() == null || request.getImageUrl().isEmpty())) {
            throw new RuntimeException("Image message must have imageUrl");
        }
        if ("FILE".equals(messageType) && (request.getFileUrl() == null || request.getFileUrl().isEmpty())) {
            throw new RuntimeException("File message must have fileUrl");
        }

        Message message = Message.builder()
                .group(group)
                .groupId(groupId)
                .senderId(residentId)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .replyToMessageId(request.getReplyToMessageId())
                .isEdited(false)
                .isDeleted(false)
                .build();

        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElse(null);
            if (replyTo != null && replyTo.getGroupId().equals(groupId)) {
                message.setReplyToMessage(replyTo);
            }
        }

        message = messageRepository.save(message);

        // Update last read time for sender (they just sent a message, so they've read it)
        member.setLastReadAt(OffsetDateTime.now());
        groupMemberRepository.save(member);

        MessageResponse response = toMessageResponse(message);
        
        // Notify via WebSocket
        notificationService.notifyNewMessage(groupId, response);
        
        // Send FCM push notification to group members (except sender)
        fcmPushService.sendChatMessageNotification(groupId, response, residentId);

        return response;
    }

    @Transactional(readOnly = true)
    public MessagePagedResponse getMessages(UUID groupId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findMessagesByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Update last read time
        member.setLastReadAt(OffsetDateTime.now());
        groupMemberRepository.save(member);

        return MessagePagedResponse.builder()
                .content(messages.getContent().stream()
                        .map(this::toMessageResponse)
                        .toList())
                .currentPage(messages.getNumber())
                .pageSize(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .isFirst(messages.isFirst())
                .isLast(messages.isLast())
                .build();
    }

    @Transactional
    public MessageResponse updateMessage(UUID groupId, UUID messageId, String content, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        if (!message.getGroupId().equals(groupId)) {
            throw new RuntimeException("Message does not belong to this group");
        }

        if (!message.getSenderId().equals(residentId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        if (!"TEXT".equals(message.getMessageType())) {
            throw new RuntimeException("Only text messages can be edited");
        }

        message.setContent(content);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        MessageResponse response = toMessageResponse(message);
        
        // Notify via WebSocket
        notificationService.notifyMessageUpdated(groupId, response);

        return response;
    }

    @Transactional
    public void deleteMessage(UUID groupId, UUID messageId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        if (!message.getGroupId().equals(groupId)) {
            throw new RuntimeException("Message does not belong to this group");
        }

        // Check if user is sender or admin/moderator
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        boolean canDelete = message.getSenderId().equals(residentId) ||
                           "ADMIN".equals(member.getRole()) ||
                           "MODERATOR".equals(member.getRole());

        if (!canDelete) {
            throw new RuntimeException("You don't have permission to delete this message");
        }

        message.setIsDeleted(true);
        message.setContent(null); // Clear content for deleted messages
        messageRepository.save(message);
        
        // Notify via WebSocket
        notificationService.notifyMessageDeleted(groupId, messageId);
    }

    public Long countUnreadMessages(UUID groupId, OffsetDateTime lastReadAt) {
        if (lastReadAt == null) {
            return messageRepository.countByGroupId(groupId);
        }
        return (long) messageRepository.findNewMessagesByGroupIdAfter(groupId, lastReadAt).size();
    }

    @Transactional
    public void markMessagesAsRead(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Update last read time to now
        member.setLastReadAt(OffsetDateTime.now());
        groupMemberRepository.save(member);
    }

    public Long getUnreadCount(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            return 0L;
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElse(null);
        
        if (member == null) {
            return 0L;
        }

        return countUnreadMessages(groupId, member.getLastReadAt());
    }

    private MessageResponse toMessageResponse(Message message) {
        Map<String, Object> senderInfo = residentInfoService.getResidentInfo(message.getSenderId());
        String senderName = senderInfo != null ? (String) senderInfo.get("fullName") : null;
        String senderAvatar = null; // TODO: Get avatar from sender info

        MessageResponse.MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroupId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .replyToMessageId(message.getReplyToMessageId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt());

        // Add reply message if exists
        if (message.getReplyToMessage() != null) {
            builder.replyToMessage(toMessageResponse(message.getReplyToMessage()));
        }

        return builder.build();
    }

    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

