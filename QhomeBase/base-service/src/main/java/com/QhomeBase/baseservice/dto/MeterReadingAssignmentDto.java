package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadingAssignmentDto(
        UUID id,
        UUID cycleId,
        String cycleName,
        UUID buildingId,
        String buildingCode,
        String buildingName,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        UUID assignedTo,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime completedAt,
        String note,
        Integer floorFrom,
        Integer floorTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

