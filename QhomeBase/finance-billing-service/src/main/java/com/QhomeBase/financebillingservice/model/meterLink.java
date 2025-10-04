package com.QhomeBase.financebillingservice.model;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema="billing", name="meter_charge_links",
        uniqueConstraints = {
                @UniqueConstraint(name="uq_mcl_invoice_line", columnNames={"tenant_id","invoice_line_id"}),
                @UniqueConstraint(name="uq_mcl_reading",      columnNames={"tenant_id","reading_id"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class meterLink {
    @Id @GeneratedValue private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_line_id", nullable=false, foreignKey=@ForeignKey(name="fk_mcl_invoice_line"))
    private invoiceLine invoiceLine;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="reading_id", nullable=false, foreignKey=@ForeignKey(name="fk_mcl_reading"))
    private meterReading reading;
}
