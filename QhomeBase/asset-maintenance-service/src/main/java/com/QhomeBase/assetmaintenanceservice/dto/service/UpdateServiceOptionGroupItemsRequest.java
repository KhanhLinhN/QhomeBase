package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceOptionGroupItemsRequest {

    @NotEmpty(message = "Group items must not be empty")
    @Valid
    private List<ServiceOptionGroupItemRequest> items;
}

