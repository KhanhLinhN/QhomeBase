package com.QhomeBase.assetmaintenanceservice.dto.service;

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
public class ServiceOptionGroupItemRequest {

    @NotNull(message = "Option ID is required")
    private UUID optionId;

    private Integer sortOrder;
}

