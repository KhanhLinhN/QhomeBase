package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.TenantDeletionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;


@Builder
public record TenantDeletionRequestDTO(
        UUID id,
        @NotNull(message = "Tenant ID cannot be null")
        UUID tenantId,
        @NotNull(message = "Requested by cannot be null")
        UUID requestedBy,
        UUID approvedBy,
        @NotBlank(message = "Reason cannot be blank")
        @Size(min = 3, max = 500)
        String reason,
        @Size(max = 500)
        String note,
        @NotNull(message = "Status cannot be null")
        TenantDeletionStatus status,
        @NotNull(message = "Created at cannot be null")
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt
) {}
