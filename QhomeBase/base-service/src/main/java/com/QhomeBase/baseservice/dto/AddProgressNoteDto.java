package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AddProgressNoteDto(
        @NotBlank(message = "Progress note is required")
        String note
) {}

