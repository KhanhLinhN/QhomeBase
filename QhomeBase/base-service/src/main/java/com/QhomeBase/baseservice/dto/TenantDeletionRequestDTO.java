package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.TenantDeletionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantDeletionRequestDTO(
        UUID id,
        UUID tenantId,
        UUID requestedBy,
        UUID approvedBy,
        String reason,
        String note,
        TenantDeletionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt
) {}
