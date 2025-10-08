package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDeletionReq(
        @NotNull UUID tenantId,
        String reason
) {}