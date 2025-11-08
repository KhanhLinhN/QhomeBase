package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceComboItemDto {

    private UUID id;
    private UUID comboId;
    private UUID includedServiceId;
    private UUID optionId;
    private Integer quantity;
    private String note;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
}

