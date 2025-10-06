package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.UserRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRolePermissionRepository extends JpaRepository<UserRolePermission, Integer> {

    @Query(value = """
  WITH roles_global AS (
    SELECT ur.role
    FROM iam.user_roles ur
    WHERE ur.user_id = :userId
  ),
  perms_global AS (
    SELECT rp.permission_code
    FROM iam.role_permissions rp
    JOIN roles_global rg ON rg.role = rp.role
  ),
  roles_local AS (
    SELECT utr.role
    FROM iam.user_tenant_roles utr
    WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId
  ),
  base_roles AS (
    SELECT tr.role, tr.base_role
    FROM iam.tenant_roles tr
    WHERE tr.tenant_id = :tenantId
  ),
  perms_from_base AS (
    SELECT rp.permission_code
    FROM base_roles br
    JOIN roles_local rl ON rl.role = br.role
    JOIN iam.role_permissions rp ON rp.role = br.base_role
  ),
  perms_local AS (
    SELECT trp.permission_code
    FROM iam.tenant_role_permissions trp
    JOIN roles_local rl ON rl.role = trp.role
    WHERE trp.tenant_id = :tenantId
  )
  SELECT DISTINCT permission_code
  FROM (
    SELECT * FROM perms_global
    UNION ALL
    SELECT * FROM perms_from_base
    UNION ALL
    SELECT * FROM perms_local
  ) u
  """, nativeQuery = true)
    List<String> getUserRolePermissionsCodeByUserIdAndTenantId(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );
}
