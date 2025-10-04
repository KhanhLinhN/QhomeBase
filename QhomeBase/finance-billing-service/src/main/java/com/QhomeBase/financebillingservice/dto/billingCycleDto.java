package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class billingCycleDto {
    UUID id;
    UUID tenantId;
    String name;
    LocalDate periodFrom;
    LocalDate periodTo;
    String status;
}
