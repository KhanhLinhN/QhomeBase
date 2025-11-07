package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RegisterServiceRequestCreateDto(
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

