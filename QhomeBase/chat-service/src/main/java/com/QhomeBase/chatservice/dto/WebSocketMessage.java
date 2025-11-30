package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // NEW_MESSAGE, MESSAGE_UPDATED, MESSAGE_DELETED, MEMBER_ADDED, MEMBER_REMOVED, GROUP_UPDATED
    private UUID groupId;
    private MessageResponse message;
    private GroupResponse group;
    private GroupMemberResponse member;
    private OffsetDateTime timestamp;
}

