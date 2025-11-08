package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CreateServiceOptionGroupRequest {

    @NotBlank(message = "Group code is required")
    @Size(max = 64, message = "Group code must not exceed 64 characters")
    private String code;

    @NotBlank(message = "Group name is required")
    @Size(max = 255, message = "Group name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Min(value = 0, message = "Minimum select must be >= 0")
    private Integer minSelect;

    @Min(value = 1, message = "Maximum select must be >= 1 when provided")
    @Max(value = 20, message = "Maximum select must be <= 20")
    private Integer maxSelect;

    private Boolean isRequired;

    private Integer sortOrder;

    @AssertTrue(message = "maxSelect must be greater than or equal to minSelect")
    public boolean isValidRange() {
        if (minSelect == null || maxSelect == null) {
            return true;
        }
        return maxSelect >= minSelect;
    }
}

