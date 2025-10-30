package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.MeterReadingSessionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadingSessionProgressDto(
        UUID sessionId,
        UUID cycleId,
        String cycleName,
        UUID buildingId,
        String buildingCode,
        UUID serviceId,
        String serviceName,
        UUID readerId,
        MeterReadingSessionStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long totalMeters,
        Long readingsCount,
        Long verifiedCount,
        Integer progressPercentage
) {}

