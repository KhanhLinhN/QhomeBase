package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "data", name = "households")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Household {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private HouseholdKind kind;

    @Column(name = "primary_resident_id")
    private UUID primaryResidentId;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

