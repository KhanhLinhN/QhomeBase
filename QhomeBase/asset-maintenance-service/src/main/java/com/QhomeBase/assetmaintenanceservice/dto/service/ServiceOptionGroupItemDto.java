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
public class ServiceOptionGroupItemDto {

    private UUID id;
    private UUID groupId;
    private UUID optionId;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
}

