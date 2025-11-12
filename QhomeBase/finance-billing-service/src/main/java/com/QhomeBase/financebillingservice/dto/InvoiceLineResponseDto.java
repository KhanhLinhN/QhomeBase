package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineResponseDto {
    private String payerUnitId;
    private String invoiceId;
    private String serviceDate;
    private String description;
    private Double quantity;
    private String unit;
    private Double unitPrice;
    private Double taxAmount;
    private Double lineTotal;
    private String serviceCode;
    private String status;
}

