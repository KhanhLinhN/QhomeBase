package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema= "billing", name="billing_cycle", uniqueConstraints = @UniqueConstraint(name="uq_billing_cycle", columnNames = {"tenant_id", "name", "period_from", "period_to"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class billingCycle {
    @Id @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name="tenant_id", nullable = false)
    private UUID  tenantId;
    @Column(name="name", nullable = false)
    private String name;
    @Column(name="period_from", nullable=false) private LocalDate periodFrom;
    @Column(name="period_to",   nullable=false) private LocalDate periodTo;

    @Column(nullable=false) private String status = "OPEN";
}
