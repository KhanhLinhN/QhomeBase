package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ResidentCardRegistrationCreateDto(
        @NotNull(message = "Unit ID is required")
        UUID unitId,

        String requestType,

        @NotNull(message = "Resident ID is required")
        UUID residentId,

        String fullName,

        @NotNull(message = "Apartment number is required")
        String apartmentNumber,

        @NotNull(message = "Building name is required")
        String buildingName,

        String citizenId,

        String phoneNumber,

        String note
) {}

