package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ResidentCardRegistrationCreateDto(
        @NotNull(message = "User ID is required")
        UUID userId,
        
        @NotNull(message = "Unit ID is required")
        UUID unitId,
        
        String requestType,
        
        @NotNull(message = "Resident ID is required")
        UUID residentId,
        
        String phoneNumber,
        
        String note
) {}

