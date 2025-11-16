package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CardRegistrationAdminDecisionRequest(
        @NotBlank(message = "decision is required")
        String decision,
        String note,
        String issueMessage
) {
}

