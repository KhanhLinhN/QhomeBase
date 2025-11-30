package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.MessageResponse;
import com.QhomeBase.chatservice.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewMessage(UUID groupId, MessageResponse message) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("NEW_MESSAGE")
                .groupId(groupId)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending NEW_MESSAGE to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    public void notifyMessageUpdated(UUID groupId, MessageResponse message) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("MESSAGE_UPDATED")
                .groupId(groupId)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending MESSAGE_UPDATED to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    public void notifyMessageDeleted(UUID groupId, UUID messageId) {
        MessageResponse deletedMessage = MessageResponse.builder()
                .id(messageId)
                .isDeleted(true)
                .build();

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("MESSAGE_DELETED")
                .groupId(groupId)
                .message(deletedMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending MESSAGE_DELETED to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    public void notifyMemberAdded(UUID groupId, com.QhomeBase.chatservice.dto.GroupMemberResponse member) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("MEMBER_ADDED")
                .groupId(groupId)
                .member(member)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending MEMBER_ADDED to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    public void notifyMemberRemoved(UUID groupId, UUID memberId) {
        com.QhomeBase.chatservice.dto.GroupMemberResponse removedMember = com.QhomeBase.chatservice.dto.GroupMemberResponse.builder()
                .id(memberId)
                .build();

        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("MEMBER_REMOVED")
                .groupId(groupId)
                .member(removedMember)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending MEMBER_REMOVED to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }

    public void notifyGroupUpdated(UUID groupId, com.QhomeBase.chatservice.dto.GroupResponse group) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("GROUP_UPDATED")
                .groupId(groupId)
                .group(group)
                .timestamp(OffsetDateTime.now())
                .build();

        String destination = "/topic/chat/" + groupId;
        log.info("Sending GROUP_UPDATED to group {} via WebSocket", groupId);
        messagingTemplate.convertAndSend(destination, wsMessage);
    }
}

