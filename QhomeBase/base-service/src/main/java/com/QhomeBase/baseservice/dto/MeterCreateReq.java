package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MeterCreateReq(
        @NotNull UUID unitId,
        @NotNull UUID serviceId,
        @NotBlank String meterCode,
        LocalDate installedAt
) {}

