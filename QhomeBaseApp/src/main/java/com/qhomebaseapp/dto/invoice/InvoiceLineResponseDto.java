package com.qhomebaseapp.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO cho Flutter response - format đơn giản với các field cần thiết
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineResponseDto {
    private String payerUnitId;
    private String invoiceId;
    private String serviceDate;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
    private String serviceCode;
    private String status;
}

