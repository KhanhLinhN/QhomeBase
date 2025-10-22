package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.UserTenantRole;
import com.QhomeBase.iamservice.model.UserTenantRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserTenantRoleRepository extends JpaRepository<UserTenantRole, UserTenantRoleId> {

    @Query(value = """
        SELECT DISTINCT utr.tenant_id
        FROM iam.user_tenant_roles utr
        WHERE utr.user_id = :userId
        """, nativeQuery = true)
    List<UUID> findTenantIdsByUserId(@Param("userId") UUID userId);

    @Query(value = """
        SELECT DISTINCT utr.role
        FROM iam.user_tenant_roles utr
        WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId
        """, nativeQuery = true)
    List<String> findRolesInTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT utr.user_id
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId AND utr.role = :role
        """, nativeQuery = true)
    List<UUID> findUserIdsByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.user_tenant_roles 
        WHERE user_id = :userId AND tenant_id = :tenantId AND role = :role
        """, nativeQuery = true)
    void deleteByUserIdAndTenantIdAndRole(@Param("userId") UUID userId, 
                                         @Param("tenantId") UUID tenantId, 
                                         @Param("role") String role);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.user_tenant_roles 
        WHERE user_id = :userId AND tenant_id = :tenantId
        """, nativeQuery = true)
    void deleteAllByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM iam.user_tenant_roles utr
        WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId AND utr.role = :role
        """, nativeQuery = true)
    boolean existsByUserIdAndTenantIdAndRole(@Param("userId") UUID userId, 
                                           @Param("tenantId") UUID tenantId, 
                                           @Param("role") String role);

    @Query(value = """
        SELECT COUNT(DISTINCT utr.user_id)
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId AND utr.role = :role
        """, nativeQuery = true)
    long countUsersByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);

    @Query(value = """
        SELECT DISTINCT utr.role
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId
        ORDER BY utr.role
        """, nativeQuery = true)
    List<String> findDistinctRolesByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
        SELECT utr.user_id, COUNT(*) as role_count
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId
        GROUP BY utr.user_id
        ORDER BY role_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findUsersWithMostRolesByTenantId(@Param("tenantId") UUID tenantId, @Param("limit") int limit);

    @Query(value = """
        SELECT DISTINCT utr.user_id
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    List<UUID> findUserIdsByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query(value = """
        SELECT DISTINCT utr.user_id
        FROM iam.user_tenant_roles utr
        """, nativeQuery = true)
    List<UUID> findAllAssignedUserIds();
    
    @Query(value = """
        SELECT COUNT(DISTINCT utr.user_id)
        FROM iam.user_tenant_roles utr
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    long countUsersInTenant(@Param("tenantId") UUID tenantId);
    
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by)
        VALUES (:userId, :tenantId, NULL, :grantedAt, :grantedBy)
        """, nativeQuery = true)
    void assignUserToTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId, @Param("grantedAt") java.time.Instant grantedAt, @Param("grantedBy") String grantedBy);
    
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM iam.user_tenant_roles 
        WHERE user_id = :userId AND tenant_id = :tenantId
        """, nativeQuery = true)
    void removeUserFromTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = """
        SELECT DISTINCT ur.role
        FROM iam.user_roles ur
        WHERE ur.user_id = :userId
        """, nativeQuery = true)
    List<String> findGlobalRolesByUserId(@Param("userId") UUID userId);

    List<UserTenantRole> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
    select utr
        from iam.user_tenant_roles utr
    where utr.tenand_id = :tenantId And utr.user_id = :userId
""", nativeQuery = true)
    List<UserTenantRole> findByUserIdAndTenantId(@Param("userId") UUID userI, @Param("tenantId") UUID tenantId);
}