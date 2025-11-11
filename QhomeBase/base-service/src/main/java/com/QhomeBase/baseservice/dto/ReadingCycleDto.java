package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ReadingCycleStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadingCycleDto(
        UUID id,
        String name,
        LocalDate periodFrom,
        LocalDate periodTo,
        ReadingCycleStatus status,
        String description,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

