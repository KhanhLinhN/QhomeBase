package com.QhomeBase.iamservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant_role_permissions", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantRolePermission {


    @Id @Column(name = "role", nullable = false)
    private String role;

    @Id @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    }