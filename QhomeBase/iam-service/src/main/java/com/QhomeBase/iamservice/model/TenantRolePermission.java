package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_role_permissions", schema = "iam",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "role", "permission_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRolePermission {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by", nullable = false)
    private String grantedBy;
}