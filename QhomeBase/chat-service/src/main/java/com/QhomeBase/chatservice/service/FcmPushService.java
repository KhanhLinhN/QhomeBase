package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.QhomeBase.chatservice.security.UserPrincipal;

import com.QhomeBase.chatservice.repository.GroupMemberRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    @Value("${customer-interaction.service.url:http://localhost:8087}")
    private String customerInteractionServiceUrl;

    private final WebClient webClient = WebClient.builder().build();
    private final GroupMemberRepository groupMemberRepository;

    /**
     * Send FCM push notification to group members when a new message is received
     * This will call customer-interaction-service to send push notifications
     */
    public void sendChatMessageNotification(UUID groupId, MessageResponse message, UUID senderId) {
        try {
            // Get all group members except sender
            List<com.QhomeBase.chatservice.model.GroupMember> members = groupMemberRepository.findByGroupId(groupId);
            
            for (com.QhomeBase.chatservice.model.GroupMember member : members) {
                if (member.getResidentId().equals(senderId)) {
                    continue; // Don't send notification to sender
                }

                // Skip if member has muted the group
                if (Boolean.TRUE.equals(member.getIsMuted())) {
                    continue;
                }

                String title = "Tin nhắn mới";
                String body = message.getSenderName() != null 
                    ? message.getSenderName() + ": " + getMessagePreview(message)
                    : "Bạn có tin nhắn mới";

                Map<String, String> data = new HashMap<>();
                data.put("type", "CHAT_MESSAGE");
                data.put("groupId", groupId.toString());
                data.put("messageId", message.getId().toString());
                data.put("senderId", senderId.toString());

                // Call customer-interaction-service to send push notification
                sendPushToResident(member.getResidentId(), title, body, data);
            }
        } catch (Exception e) {
            log.error("Error sending FCM push notification for chat message: {}", e.getMessage(), e);
        }
    }

    private String getMessagePreview(MessageResponse message) {
        if (message.getIsDeleted() != null && message.getIsDeleted()) {
            return "Tin nhắn đã bị xóa";
        }
        
        if ("IMAGE".equals(message.getMessageType())) {
            return "📷 Đã gửi một hình ảnh";
        }
        
        if ("FILE".equals(message.getMessageType())) {
            return "📎 Đã gửi một tệp";
        }
        
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String content = message.getContent();
            if (content.length() > 100) {
                return content.substring(0, 100) + "...";
            }
            return content;
        }
        
        return "Tin nhắn mới";
    }

    public void sendPushToResident(UUID residentId, String title, String body, Map<String, String> data) {
        try {
            // Use push-only endpoint which only sends FCM push without saving to notification table
            // This is for chat messages that should appear in chat screen, not in notification list
            String url = customerInteractionServiceUrl + "/api/notifications/push-only";
            
            Map<String, Object> request = new HashMap<>();
            request.put("residentId", residentId.toString());
            request.put("title", title);
            request.put("message", body);
            if (data != null) {
                request.put("data", data);
            }

            webClient
                    .post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.debug("Sent FCM push-only notification to resident: {} (not saved to DB)", residentId);
        } catch (Exception e) {
            log.error("Error sending FCM push to resident {}: {}", residentId, e.getMessage(), e);
        }
    }

    /**
     * Send FCM push notification for direct message
     */
    public void sendDirectMessageNotification(UUID recipientId, UUID conversationId, com.QhomeBase.chatservice.dto.DirectMessageResponse message) {
        try {
            String title = "Tin nhắn mới";
            String body = message.getSenderName() != null 
                ? message.getSenderName() + ": " + getDirectMessagePreview(message)
                : "Bạn có tin nhắn mới";

            Map<String, String> data = new HashMap<>();
            data.put("type", "DIRECT_MESSAGE");
            data.put("conversationId", conversationId.toString());
            data.put("messageId", message.getId().toString());
            data.put("senderId", message.getSenderId() != null ? message.getSenderId().toString() : "");

            sendPushToResident(recipientId, title, body, data);
        } catch (Exception e) {
            log.error("Error sending FCM push notification for direct message: {}", e.getMessage(), e);
        }
    }

    /**
     * Send FCM push notification for direct invitation
     */
    public void sendDirectInvitationNotification(UUID inviteeId, UUID inviterId, UUID conversationId) {
        try {
            String title = "Lời mời trò chuyện";
            String body = "Bạn có lời mời trò chuyện mới";

            Map<String, String> data = new HashMap<>();
            data.put("type", "DIRECT_INVITATION");
            data.put("conversationId", conversationId.toString());
            data.put("inviterId", inviterId.toString());

            sendPushToResident(inviteeId, title, body, data);
        } catch (Exception e) {
            log.error("Error sending FCM push notification for direct invitation: {}", e.getMessage(), e);
        }
    }

    private String getDirectMessagePreview(com.QhomeBase.chatservice.dto.DirectMessageResponse message) {
        if (message.getIsDeleted() != null && message.getIsDeleted()) {
            return "Tin nhắn đã bị xóa";
        }
        
        if ("IMAGE".equals(message.getMessageType())) {
            return "📷 Đã gửi một hình ảnh";
        }
        
        if ("FILE".equals(message.getMessageType())) {
            return "📎 Đã gửi một tệp";
        }

        if ("AUDIO".equals(message.getMessageType())) {
            return "🎤 Đã gửi một tin nhắn thoại";
        }
        
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String content = message.getContent();
            if (content.length() > 100) {
                return content.substring(0, 100) + "...";
            }
            return content;
        }
        
        return "Tin nhắn mới";
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

