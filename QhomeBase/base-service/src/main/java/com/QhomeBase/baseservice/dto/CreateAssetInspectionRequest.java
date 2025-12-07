package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAssetInspectionRequest(
        UUID contractId,
        UUID unitId,
        LocalDate inspectionDate,
        String inspectorName,
        UUID inspectorId
) {
}

