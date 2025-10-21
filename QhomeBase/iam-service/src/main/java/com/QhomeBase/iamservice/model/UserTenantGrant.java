package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_tenant_grants", schema = "iam",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id", "permission_code"}))
@IdClass(UserTenantGrantId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTenantGrant {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by", nullable = false)
    private String grantedBy;

    @Column(name = "reason")
    private String reason;
}

