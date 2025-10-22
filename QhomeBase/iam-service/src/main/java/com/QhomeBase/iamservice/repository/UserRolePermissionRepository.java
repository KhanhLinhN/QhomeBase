package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRolePermissionRepository extends JpaRepository<RolePermission, Integer> {


    @Query(value = """
        WITH global_perms AS (
            SELECT rp.permission_code
            FROM iam.user_roles ur
            JOIN iam.role_permissions rp ON ur.role = rp.role
            WHERE ur.user_id = :userId
        ),
        tenant_role_perms AS (
            SELECT trp.permission_code
            FROM iam.user_tenant_roles utr
            JOIN iam.tenant_role_permissions trp ON utr.tenant_id = trp.tenant_id AND utr.role = trp.role
            WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId AND trp.granted = true
        ),
        tenant_role_denies AS (
            SELECT trp.permission_code
            FROM iam.user_tenant_roles utr
            JOIN iam.tenant_role_permissions trp ON utr.tenant_id = trp.tenant_id AND utr.role = trp.role
            WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId AND trp.granted = false
        ),
        user_grants AS (
            SELECT utg.permission_code
            FROM iam.user_tenant_grants utg
            WHERE utg.user_id = :userId AND utg.tenant_id = :tenantId 
              AND (utg.expires_at IS NULL OR utg.expires_at > now())
        ),
        user_denies AS (
            SELECT utd.permission_code
            FROM iam.user_tenant_denies utd
            WHERE utd.user_id = :userId AND utd.tenant_id = :tenantId
              AND (utd.expires_at IS NULL OR utd.expires_at > now())
        ),
        final_perms AS (
            SELECT DISTINCT perm.permission_code
            FROM (
                SELECT permission_code FROM global_perms
                UNION ALL
                SELECT permission_code FROM tenant_role_perms
                UNION ALL
                SELECT permission_code FROM user_grants
            ) perm
            WHERE perm.permission_code NOT IN (
                SELECT permission_code FROM user_denies
                UNION
                SELECT permission_code FROM tenant_role_denies
            )
        )
        SELECT permission_code FROM final_perms
        """, nativeQuery = true)
    List<String> getUserRolePermissionsCodeByUserIdAndTenantId(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );



}
