package com.QhomeBase.baseservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MaintenanceRequestDto(
        UUID id,
        UUID unitId,
        UUID residentId,
        UUID userId,
        UUID createdBy,
        String category,
        String title,
        String description,
        List<String> attachments,
        String location,
        OffsetDateTime preferredDatetime,
        String contactName,
        String contactPhone,
        String note,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


