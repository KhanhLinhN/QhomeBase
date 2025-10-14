package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.model.building;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UnitUpdateDto(

        @NotNull(message = "Floor is required")
        @Positive(message = "Floor must be positive")
        Integer floor,

        @NotNull(message = "Area is required")
        @Positive(message = "Area must be positive")
        BigDecimal areaM2,

        @NotNull(message = "Bedrooms is required")
        @Positive(message = "Bedrooms must be positive")
        Integer bedrooms
) {}
