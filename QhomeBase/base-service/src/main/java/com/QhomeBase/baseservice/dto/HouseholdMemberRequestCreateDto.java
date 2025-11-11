package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberRequestCreateDto(
        @NotNull(message = "Household ID is required")
        UUID householdId,

        @NotBlank(message = "Resident full name is required")
        String residentFullName,

        String residentPhone,

        String residentEmail,

        String residentNationalId,

        LocalDate residentDob,

        String relation,

        String proofOfRelationImageUrl,

        String note
) {}
