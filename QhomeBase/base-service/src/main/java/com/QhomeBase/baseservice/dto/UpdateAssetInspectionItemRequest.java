package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record UpdateAssetInspectionItemRequest(
        String conditionStatus,
        String notes,
        Boolean checked,
        UUID checkedBy
) {}

