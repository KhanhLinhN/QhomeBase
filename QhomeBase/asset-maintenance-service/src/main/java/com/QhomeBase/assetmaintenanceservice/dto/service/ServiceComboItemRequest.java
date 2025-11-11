package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceComboItemRequest {

    private UUID includedServiceId;

    private UUID optionId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private String note;

    private Integer sortOrder;

    @AssertTrue(message = "Either includedServiceId or optionId must be provided")
    public boolean isValidTarget() {
        return (includedServiceId != null) ^ (optionId != null);
    }
}

