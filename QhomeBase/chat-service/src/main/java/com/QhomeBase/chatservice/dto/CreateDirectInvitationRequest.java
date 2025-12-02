package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDirectInvitationRequest {
    private UUID inviteeId; // Resident ID to invite
    private String initialMessage; // Optional first message
}

