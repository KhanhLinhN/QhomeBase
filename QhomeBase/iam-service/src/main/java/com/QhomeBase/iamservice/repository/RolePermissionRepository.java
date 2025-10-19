package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<Permission, String> {

    @Query(value = """
        SELECT p.code, p.description
        FROM iam.permissions p
        JOIN iam.role_permissions rp ON p.code = rp.permission_code
        WHERE rp.role = :role
        """, nativeQuery = true)
    List<Object[]> findPermissionsByRole(@Param("role") String role);

    @Query(value = """
        SELECT p.code, p.description
        FROM iam.permissions p
        JOIN iam.role_permissions rp ON p.code = rp.permission_code
        WHERE rp.role = :role
        """, nativeQuery = true)
    List<Permission> findPermissionObjectsByRole(@Param("role") String role);
}
