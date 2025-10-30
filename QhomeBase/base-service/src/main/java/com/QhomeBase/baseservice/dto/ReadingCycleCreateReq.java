package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ReadingCycleCreateReq(
        @NotBlank String name,
        @NotNull LocalDate periodFrom,
        @NotNull LocalDate periodTo,
        String description,
        UUID createdBy
) {}

