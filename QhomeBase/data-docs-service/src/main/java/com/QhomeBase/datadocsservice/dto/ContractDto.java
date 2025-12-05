package com.QhomeBase.datadocsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto {

    private UUID id;
    private UUID unitId;
    private String contractNumber;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate checkoutDate;
    private BigDecimal monthlyRent;
    private BigDecimal purchasePrice;
    private String paymentMethod;
    private String paymentTerms;
    private LocalDate purchaseDate;
    private String notes;
    private String status;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private List<ContractFileDto> files;
}

