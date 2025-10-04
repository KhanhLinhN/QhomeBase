package com.QhomeBase.financebillingservice.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "billing", name = "invoices", uniqueConstraints = @UniqueConstraint(name = "uq_invoices_per_tenant", columnNames = {"tenant_id", "code"}),
indexes =@Index(name="idx_invoices_tenant_status", columnList = "tenant_id,status"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class invoice {
    @Id
    @GeneratedValue private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;

    @Column(nullable=false) private String code;

    @Column(name="issued_at", nullable=false) private Instant issuedAt;

    @Column(name="due_date") private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable=false) private enums.InvStatus status = enums.InvStatus.DRAFT;

    @Column(name="currency",nullable=false) private String currency = "VND";

    @Column(name="bill_to_name")    private String billToName;
    @Column(name="bill_to_address") private String billToAddress;
    @Column(name="bill_to_contact") private String billToContact;

    @Column(name="payer_unit_id")     private UUID payerUnitId;
    @Column(name="payer_resident_id") private UUID payerResidentId;
    @ManyToOne(fetch=FetchType.LAZY,optional = false)
    @JoinColumn(name="cycle_id", foreignKey=@ForeignKey(name="fk_invoice_cycle"))
    private billingCycle cycle;
    @PrePersist void pre() { if (issuedAt == null) issuedAt = Instant.now(); }
}
