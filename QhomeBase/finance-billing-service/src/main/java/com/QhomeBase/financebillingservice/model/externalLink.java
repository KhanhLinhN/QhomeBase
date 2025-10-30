package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class externalLink {
    @Id @GeneratedValue private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_id", foreignKey=@ForeignKey(name="fk_extlink_invoice"))
    private invoice invoice;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_line_id", foreignKey=@ForeignKey(name="fk_extlink_invoice_line"))
    private invoiceLine invoiceLine;

    @Column(name="ref_type", nullable=false) private String refType;
    @Column(name="ref_id",   nullable=false) private String refId;

    @Column private String note;


    @AssertTrue
    public boolean isXorValid() {
        return (invoice != null) ^ (invoiceLine != null);
    }
}
