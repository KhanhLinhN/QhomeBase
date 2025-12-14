package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberRequestCreateDto(
        @NotNull(message = "Household ID is required")
        UUID householdId,

        @NotBlank(message = "Resident full name is required")
        @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên không được chứa số hoặc ký tự đặc biệt")
        String residentFullName,

        String residentPhone,

        String residentEmail,

        @Pattern(regexp = "^[^\\s]+$", message = "CCCD không được chứa khoảng trắng")
        String residentNationalId,

        LocalDate residentDob,

        String relation,

        String proofOfRelationImageUrl,

        String note
) {}
