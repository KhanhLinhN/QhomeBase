package com.QhomeBase.financebillingservice.model;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
@Entity
@Table(schema="billing", name="meter_readings",
        uniqueConstraints=@UniqueConstraint(name="uq_meter_reading",
                columnNames={"tenant_id","unit_id","service_code","reading_date"}),
        indexes=@Index(name="idx_meter_readings_unit_service_date",
                columnList="tenant_id, unit_id, service_code, reading_date DESC"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder

public class meterReading {
    @Id @GeneratedValue private UUID id;

    @Column(name="tenant_id", nullable=false) private UUID tenantId;

    @Column(name="unit_id", nullable=false) private UUID unitId;

    @Column(name="service_code", nullable=false) private String serviceCode;

    @Column(name="reading_date", nullable=false) private LocalDate readingDate;

    @Column(name="prev_index", nullable=false, precision=14, scale=3) private BigDecimal prevIndex;

    @Column(name="curr_index", nullable=false, precision=14, scale=3) private BigDecimal currIndex;

    @Column(name="photo_file_id") private UUID photoFileId;

    @Column private String note;
}
