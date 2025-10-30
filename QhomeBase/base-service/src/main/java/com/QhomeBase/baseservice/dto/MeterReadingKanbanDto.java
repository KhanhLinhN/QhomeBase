package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadingKanbanDto(
        UUID cycleId,
        String cycleName,
        UUID assignmentId,
        UUID buildingId,
        String buildingCode,
        String buildingName,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        UUID assignedTo,
        LocalDate dueDate,
        Integer floorFrom,
        Integer floorTo,
        String kanbanStatus,
        Integer totalMeters,
        Integer readingsCount,
        Integer verifiedCount,
        Integer progressPercentage,
        Boolean isOverdue,
        Boolean hasAnomaly,
        OffsetDateTime sessionStartedAt,
        OffsetDateTime sessionCompletedAt,
        OffsetDateTime lastVerifiedAt,
        OffsetDateTime invoiceGeneratedAt
) {}

