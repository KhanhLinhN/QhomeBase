package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleRegistrationRejectDto(
        @NotNull(message = "Rejected by is required")
        UUID rejectedBy,

        @NotNull(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {}


