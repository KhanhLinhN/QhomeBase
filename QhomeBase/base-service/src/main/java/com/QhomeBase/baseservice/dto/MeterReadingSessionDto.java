package com.QhomeBase.baseservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadingSessionDto(
        UUID id,
        UUID assignmentId,
        UUID cycleId,
        UUID buildingId,
        UUID serviceId,
        UUID readerId,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer unitsRead,
        String deviceInfo,
        Boolean isCompleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
