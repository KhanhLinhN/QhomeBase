package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.TenantRole;
import com.QhomeBase.iamservice.model.TenantRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface UserTenantRoleRepository extends JpaRepository<TenantRole, TenantRoleId> {

    @Query(value = """
      select distinct utr.tenant_id
      from iam.user_tenant_roles utr
      where utr.user_id = :userId
      """, nativeQuery = true)
    List<UUID> findTenantIdsByUserId(@Param("userId") UUID userId);

    @Query(value = """
      select utr.role
      from iam.user_tenant_roles utr
      where utr.user_id = :userId and utr.tenant_id = :tenantId
      """, nativeQuery = true)
    List<String> findRolesInTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query(value = """
      select utr.user_id
      from iam.user_tenant_roles utr
      where utr.tenant_id = :tenantId and utr.role = :role
      """, nativeQuery = true)
    List<UUID> findUsersByRoleInTenant(@Param("tenantId") UUID tenantId, @Param("role") String role);
}