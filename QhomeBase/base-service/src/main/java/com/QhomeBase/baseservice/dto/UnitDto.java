package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.UnitStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UnitDto(
        UUID id,
        UUID tenantId,
        UUID buildingId,
        String buildingCode,
        String buildingName,
        String code,
        Integer floor,
        BigDecimal areaM2,
        Integer bedrooms,
        UnitStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}