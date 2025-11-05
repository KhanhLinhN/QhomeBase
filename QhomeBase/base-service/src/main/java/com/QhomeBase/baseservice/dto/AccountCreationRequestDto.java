package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.AccountCreationRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountCreationRequestDto(
        UUID id,
        UUID residentId,
        String residentName,
        String residentEmail,
        String residentPhone,
        UUID requestedBy,
        String requestedByName,
        String username,
        String email,
        boolean autoGenerate,
        AccountCreationRequest.RequestStatus status,
        UUID approvedBy,
        String approvedByName,
        UUID rejectedBy,
        String rejectedByName,
        String rejectionReason,
        OffsetDateTime approvedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime createdAt
) {}

