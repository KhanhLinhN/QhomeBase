package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RegisterServiceRequestCreateDto(
        @NotNull(message = "User ID is required")
        UUID userId,
        
        @NotBlank(message = "Service type is required")
        String serviceType,
        
        String requestType,
        
        String note,
        
        String vehicleType,
        
        String licensePlate,
        
        String vehicleBrand,
        
        String vehicleColor,
        
        List<String> imageUrls
) {}

