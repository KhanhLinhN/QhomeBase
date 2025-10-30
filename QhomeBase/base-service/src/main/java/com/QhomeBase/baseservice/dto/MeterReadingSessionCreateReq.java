package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MeterReadingSessionCreateReq(
        @NotNull(message = "Assignment ID is required")
        UUID assignmentId,
        
        String deviceInfo
) {}
