package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.MeterReadingSessionStatus;
import jakarta.validation.constraints.NotNull;

public record MeterReadingSessionCompleteReq(
        @NotNull MeterReadingSessionStatus status
) {}

