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
    

    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    List<User> findByTenantId(@Param("tenantId") UUID tenantId);
    

    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId AND utr.role = :roleName
        """, nativeQuery = true)
    List<User> findByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("roleName") String roleName);
    

    Optional<User> findByUsername(String username);
    

    Optional<User> findByEmail(String email);
    

    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE u.username = :username AND utr.tenant_id = :tenantId
        """, nativeQuery = true)
    Optional<User> findByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") UUID tenantId);
    

    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE u.email = :email AND utr.tenant_id = :tenantId
        """, nativeQuery = true)
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);
    

    @Query(value = """
        SELECT COUNT(DISTINCT u.id) FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId
        """, nativeQuery = true)
    long countByTenantId(@Param("tenantId") UUID tenantId);
    

    @Query(value = """
        SELECT DISTINCT u.* FROM iam.users u
        JOIN iam.user_tenant_roles utr ON u.id = utr.user_id
        WHERE utr.tenant_id = :tenantId 
        AND u.active = true
        ORDER BY u.created_at DESC
        """, nativeQuery = true)
    List<User> findActiveUsersByTenantId(@Param("tenantId") UUID tenantId);


    @Query(value = """
        SELECT DISTINCT u.* 
        FROM iam.users u
        JOIN iam.user_roles ur ON u.id = ur.user_id
        WHERE ur.role IN ('technician', 'supporter', 'account') 
        AND u.id NOT IN (
            SELECT DISTINCT user_id 
            FROM iam.user_tenant_roles 
            WHERE tenant_id IS NOT NULL
        )
        AND u.active = true
        """, nativeQuery = true)
    List<User> findAvailableStaff();

    
}