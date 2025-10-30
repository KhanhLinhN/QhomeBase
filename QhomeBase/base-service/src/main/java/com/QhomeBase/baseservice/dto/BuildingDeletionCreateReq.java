package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BuildingDeletionCreateReq(
        @NotNull UUID tenantId,
        @NotNull UUID buildingId,
        String reason
) {}