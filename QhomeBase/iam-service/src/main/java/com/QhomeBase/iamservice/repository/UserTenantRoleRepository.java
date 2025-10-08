package com.QhomeBase.iamservice.repository;




import com.QhomeBase.iamservice.model.TenantRole;
import com.QhomeBase.iamservice.model.TenantRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;


public interface UserTenantRoleRepository extends JpaRepository<TenantRole, TenantRoleId> {
    @Query(value = """
SELECT role
FROM iam.user_tenant_roles
WHERE user_id = :uid AND tenant_id = :tid
""", nativeQuery = true)
    List<String> findRolesByUserAndTenant(@Param("uid") UUID userId, @Param("tid") UUID tenantId);


    @Query(value = """
     SELECT utr.user_id
          FROM iam.user_tenant_roles utr
          WHERE utr.tenant_id = :tenantId
            AND utr.role IN ('tenant_manager','tenant_owner')
           
""", nativeQuery = true)
    List<UUID> findManagerIdsByTenant(@Param("tid") UUID tenantId);
}