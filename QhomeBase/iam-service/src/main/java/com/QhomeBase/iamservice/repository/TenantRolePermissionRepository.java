package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.TenantRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TenantRolePermissionRepository extends JpaRepository<TenantRolePermission, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO iam.tenant_role_permissions (tenant_id, role, permission_code, granted, granted_at, granted_by)
        VALUES (:tenantId, :role, :permissionCode, :granted, :grantedAt, :grantedBy)
        ON CONFLICT (tenant_id, role, permission_code) DO UPDATE SET
            granted = EXCLUDED.granted,
            granted_at = EXCLUDED.granted_at,
            granted_by = EXCLUDED.granted_by
        """, nativeQuery = true)
    void upsertPermission(@Param("tenantId") UUID tenantId,
                          @Param("role") String role,
                          @Param("permissionCode") String permissionCode,
                          @Param("granted") boolean granted,
                          @Param("grantedAt") Instant grantedAt,
                          @Param("grantedBy") String grantedBy);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId AND role = :role AND permission_code = :permissionCode
        """, nativeQuery = true)
    void removePermission(@Param("tenantId") UUID tenantId,
                          @Param("role") String role,
                          @Param("permissionCode") String permissionCode);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId AND role = :role
        """, nativeQuery = true)
    void removeAllPermissionsForRole(@Param("tenantId") UUID tenantId,
                                     @Param("role") String role);

    @Query(value = """
        SELECT permission_code
        FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId AND role = :role AND granted = true
        """, nativeQuery = true)
    List<String> findGrantedPermissionsByTenantAndRole(@Param("tenantId") UUID tenantId,
                                                        @Param("role") String role);

    @Query(value = """
        SELECT permission_code
        FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId AND role = :role AND granted = false
        """, nativeQuery = true)
    List<String> findDeniedPermissionsByTenantAndRole(@Param("tenantId") UUID tenantId,
                                                       @Param("role") String role);

    @Query(value = """
        SELECT permission_code, granted
        FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId AND role = :role
        """, nativeQuery = true)
    List<Object[]> findPermissionsByTenantAndRole(@Param("tenantId") UUID tenantId,
                                                   @Param("role") String role);

    @Query(value = """
        SELECT DISTINCT role
        FROM iam.tenant_role_permissions
        WHERE tenant_id = :tenantId
        """, nativeQuery = true)
    List<String> findRolesWithPermissionsInTenant(@Param("tenantId") UUID tenantId);




    @Query(value = """
        WITH tenant_roles AS (
            
            SELECT DISTINCT role
            FROM iam.user_tenant_roles
            WHERE tenant_id = :tenantId
        ),
        global_permissions AS (
          
            SELECT DISTINCT rp.permission_code
            FROM tenant_roles tr
            JOIN iam.role_permissions rp ON tr.role = rp.role
        ),
        tenant_grants AS (
           
            SELECT DISTINCT permission_code
            FROM iam.tenant_role_permissions
            WHERE tenant_id = :tenantId AND granted = true
        ),
        tenant_denies AS (
           
            SELECT DISTINCT permission_code
            FROM iam.tenant_role_permissions
            WHERE tenant_id = :tenantId AND granted = false
        )
        -- Combine global + grants, then exclude denies
        SELECT DISTINCT permission_code
        FROM (
            SELECT permission_code FROM global_permissions
            UNION
            SELECT permission_code FROM tenant_grants
        ) combined
        WHERE permission_code NOT IN (SELECT permission_code FROM tenant_denies)
        ORDER BY permission_code
        """, nativeQuery = true)
    List<String> findAllEffectivePermissionsInTenant(@Param("tenantId") UUID tenantId);

}



