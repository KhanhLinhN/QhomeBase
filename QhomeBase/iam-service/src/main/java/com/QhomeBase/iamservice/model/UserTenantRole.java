package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_tenant_roles", schema = "iam")
@IdClass(UserTenantRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTenantRole {
    
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Id
    @Column(name = "role", nullable = false)
    private String role;
    
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
    
    @Column(name = "granted_by")
    private String grantedBy;
}

