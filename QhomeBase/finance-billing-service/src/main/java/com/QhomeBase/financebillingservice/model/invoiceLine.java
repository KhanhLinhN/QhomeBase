package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema="billing", name="invoice_lines",
        indexes=@Index(name="idx_invoice_lines_invoice", columnList="invoice_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class invoiceLine {
    @Id @GeneratedValue private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;



    @Column(name="service_date") private LocalDate serviceDate;

    @Column(nullable=false) private String description;

    @Column(nullable=false, precision=14, scale=4) private BigDecimal quantity = BigDecimal.ONE;

    @Column private String unit;

    @Column(name="unit_price", nullable=false, precision=14, scale=4) private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name="tax_rate",   nullable=false, precision=5,  scale=2) private BigDecimal taxRate = new BigDecimal("0.00");

    @Column(name="tax_amount", nullable=false, precision=14, scale=4) private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name="service_code")      private String serviceCode;
    @Column(name="external_ref_type") private String externalRefType;
    @Column(name="external_ref_id")   private String externalRefId;
}
