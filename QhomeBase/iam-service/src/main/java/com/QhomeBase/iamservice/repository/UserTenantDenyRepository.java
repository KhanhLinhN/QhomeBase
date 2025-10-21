package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.UserTenantDeny;
import com.QhomeBase.iamservice.model.UserTenantDenyId;
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
public interface UserTenantDenyRepository extends JpaRepository<UserTenantDeny, UserTenantDenyId> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO iam.user_tenant_denies (user_id, tenant_id, permission_code, expires_at, granted_at, granted_by, reason)
        VALUES (:userId, :tenantId, :permissionCode, :expiresAt, :grantedAt, :grantedBy, :reason)
        ON CONFLICT (user_id, tenant_id, permission_code) DO UPDATE SET
            expires_at = EXCLUDED.expires_at,
            granted_at = EXCLUDED.granted_at,
            granted_by = EXCLUDED.granted_by,
            reason = EXCLUDED.reason
        """, nativeQuery = true)
    void upsertDeny(@Param("userId") UUID userId,
                    @Param("tenantId") UUID tenantId,
                    @Param("permissionCode") String permissionCode,
                    @Param("expiresAt") Instant expiresAt,
                    @Param("grantedAt") Instant grantedAt,
                    @Param("grantedBy") String grantedBy,
                    @Param("reason") String reason);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.user_tenant_denies
        WHERE user_id = :userId AND tenant_id = :tenantId AND permission_code = :permissionCode
        """, nativeQuery = true)
    void removeDeny(@Param("userId") UUID userId,
                    @Param("tenantId") UUID tenantId,
                    @Param("permissionCode") String permissionCode);

    @Query(value = """
        SELECT permission_code
        FROM iam.user_tenant_denies
        WHERE user_id = :userId AND tenant_id = :tenantId
          AND (expires_at IS NULL OR expires_at > now())
        """, nativeQuery = true)
    List<String> findActiveDeniesByUserAndTenant(@Param("userId") UUID userId,
                                                 @Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT permission_code, expires_at, granted_at, granted_by, reason
        FROM iam.user_tenant_denies
        WHERE user_id = :userId AND tenant_id = :tenantId
        ORDER BY granted_at DESC
        """, nativeQuery = true)
    List<Object[]> findDeniesByUserAndTenant(@Param("userId") UUID userId,
                                             @Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT COUNT(*)
        FROM iam.user_tenant_denies
        WHERE user_id = :userId AND tenant_id = :tenantId
          AND (expires_at IS NULL OR expires_at > now())
        """, nativeQuery = true)
    int countActiveDeniesByUserAndTenant(@Param("userId") UUID userId,
                                         @Param("tenantId") UUID tenantId);
}

