package com.qhomebaseapp.dto.invoice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDto {
    private String id;
    private String code;
    private Instant issuedAt;
    private LocalDate dueDate;
    private String status;
    private String currency;
    private String billToName;
    private String billToAddress;
    private String billToContact;
    private String payerUnitId;
    private String payerResidentId;
    private String cycleId;
    private BigDecimal totalAmount;
    private List<InvoiceLineDto> lines;
}

