package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MeterReadingCreateReq(
        @NotNull UUID meterId,
        UUID sessionId,
        @NotNull LocalDate readingDate,
        @NotNull @PositiveOrZero BigDecimal prevIndex,
        @NotNull @PositiveOrZero BigDecimal currIndex,
        UUID photoFileId,
        String note,
        @NotNull UUID readerId
) {}

