package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Tìm user theo tenant ID thông qua user_tenant_roles
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Tìm user theo tenant ID và role
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId AND utr.role = :roleName
        """, nativeQuery = true)
    List<User> findByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("roleName") String roleName);
    
    /**
     * Tìm user theo username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Tìm user theo email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Tìm user theo username và tenant ID thông qua user_tenant_roles
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE u.username = :username AND utr.tenant_id = :tenantId
        """, nativeQuery = true)
    Optional<User> findByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") UUID tenantId);
    
    /**
     * Tìm user theo email và tenant ID thông qua user_tenant_roles
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE u.email = :email AND utr.tenant_id = :tenantId
        """, nativeQuery = true)
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);
    
    /**
     * Đếm số user trong tenant thông qua user_tenant_roles
     */
    @Query(value = """
        SELECT COUNT(DISTINCT u.id) FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);
    
    /**
     * Tìm user active trong tenant thông qua user_tenant_roles
     */
    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId 
        AND u.active = true
        ORDER BY u.created_at DESC
        """, nativeQuery = true)
    List<User> findActiveUsersByTenantId(@Param("tenantId") UUID tenantId);
    
}