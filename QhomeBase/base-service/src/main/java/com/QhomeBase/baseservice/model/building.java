package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "buildings", schema = "data", uniqueConstraints = @UniqueConstraint(name = "uq_buildings_tenant_code", columnNames = {"tenant_id, code"}),
indexes = @Index(name="idx_buildings_tenant",columnList = "tenant_id"))
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class building {
    @GeneratedValue
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    public UUID getId() {
        return this.id;
    }
}
