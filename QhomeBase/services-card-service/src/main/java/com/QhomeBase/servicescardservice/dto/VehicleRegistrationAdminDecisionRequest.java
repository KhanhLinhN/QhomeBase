package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationAdminDecisionRequest {

    @NotBlank(message = "decision is required")
    private String decision;

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String note;

    @Size(max = 2000, message = "Thông điệp gửi cư dân không được vượt quá 2000 ký tự")
    private String issueMessage;
}

