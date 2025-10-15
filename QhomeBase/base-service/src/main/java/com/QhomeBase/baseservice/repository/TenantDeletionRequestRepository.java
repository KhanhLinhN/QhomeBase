package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import com.QhomeBase.baseservice.model.TenantDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantDeletionRequestRepository extends JpaRepository<TenantDeletionRequest, UUID> {
    Optional<TenantDeletionRequest> findById(UUID id);

    @Query(value = """
    SELECT td.* FROM data.tenant_deletion_requests td
    WHERE td.tenant_id = :tenantId 
    AND td.status = 'PENDING'
    ORDER BY td.created_at DESC
    LIMIT 1
    """, nativeQuery = true)
    Optional<TenantDeletionRequest> findPendingRequestByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
    SELECT td.* FROM data.tenant_deletion_requests td
    WHERE td.status = :status
    ORDER BY td.created_at DESC
    """, nativeQuery = true)
    java.util.List<TenantDeletionRequest> findByStatus(@Param("status") String status);

    @Query(value = """
    SELECT td.* FROM data.tenant_deletion_requests td
    WHERE td.requested_by = :requestedBy
    ORDER BY td.created_at DESC
    """, nativeQuery = true)
    java.util.List<TenantDeletionRequest> findByRequestedBy(@Param("requestedBy") UUID requestedBy);

    @Query(value = """
    SELECT COUNT(*) FROM data.tenant_deletion_requests td
    WHERE td.tenant_id = :tenantId 
    AND td.status = 'PENDING'
    """, nativeQuery = true)
    long countPendingRequestsByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
    SELECT COUNT(*) FROM data.tenant_deletion_requests td
    JOIN data.tenants t ON td.tenant_id = t.id
    WHERE td.tenant_id = :tenantId 
    AND t.is_deleted = true
    LIMIT 1
    """, nativeQuery = true)
    long findDeletedTenant(@Param("tenantId") UUID tenantId);




}
