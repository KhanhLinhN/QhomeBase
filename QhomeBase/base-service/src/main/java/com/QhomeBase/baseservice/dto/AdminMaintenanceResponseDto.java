package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminMaintenanceResponseDto(
        @NotBlank(message = "Admin response is required")
        String adminResponse,
        
        @NotNull(message = "Estimated cost is required")
        @DecimalMin(value = "0.0", message = "Estimated cost must be non-negative")
        BigDecimal estimatedCost,
        
        String note
) {
}

