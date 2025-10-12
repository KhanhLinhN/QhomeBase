package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record BuildingDto(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String address,
        Integer floorsMax,
        Integer totalApartmentsAll,
        Integer totalApartmentsActive
) {}