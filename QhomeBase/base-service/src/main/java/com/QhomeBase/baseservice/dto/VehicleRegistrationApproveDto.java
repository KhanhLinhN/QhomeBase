package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleRegistrationApproveDto(
        @NotNull(message = "Approved by is required")
        UUID approvedBy,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}

