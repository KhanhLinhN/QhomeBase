package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "data", name = "units",
        uniqueConstraints = @UniqueConstraint(name = "uq_units_tenant_building_code",
                columnNames = {"tenant_id", "building_id", "code"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_units_building"))
    private building building;

    @Column(nullable = false)
    private String code;

    @Column
    private Integer floor;

    @Column(name = "area_m2", precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column
    private Integer bedrooms;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
