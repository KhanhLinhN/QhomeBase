package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOptionGroupDto {

    private UUID id;
    private UUID serviceId;
    private String code;
    private String name;
    private String description;
    private Integer minSelect;
    private Integer maxSelect;
    private Boolean isRequired;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ServiceOptionGroupItemDto> items;
}

