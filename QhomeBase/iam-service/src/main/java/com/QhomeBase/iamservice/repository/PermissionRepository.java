package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    @Query("SELECT p FROM Permission p WHERE p.code = :code")
    Optional<Permission> findByCode(@Param("code") String code);

    @Query("SELECT p FROM Permission p WHERE p.code LIKE :prefix%")
    List<Permission> findByCodePrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'base.%'")
    List<Permission> findBaseServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'iam.%'")
    List<Permission> findIamServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'maintenance.%'")
    List<Permission> findMaintenanceServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'finance.%'")
    List<Permission> findFinanceServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'document.%'")
    List<Permission> findDocumentServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'report.%'")
    List<Permission> findReportServicePermissions();

    @Query("SELECT p FROM Permission p WHERE p.code LIKE 'system.%'")
    List<Permission> findSystemServicePermissions();
}
