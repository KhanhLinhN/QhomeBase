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
public class UpdateServiceBookingSlotsRequest {

    @NotEmpty(message = "Slots must not be empty")
    @Valid
    private List<ServiceBookingSlotRequest> slots;
}

