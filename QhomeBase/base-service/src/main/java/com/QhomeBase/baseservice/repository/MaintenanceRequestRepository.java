package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {
    List<MaintenanceRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    List<MaintenanceRequest> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    long countByResidentId(UUID residentId);

    long countByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    @Query(value = "select * from data.maintenance_requests " +
            "where resident_id = :residentId " +
            "order by created_at desc " +
            "limit :limit offset :offset", nativeQuery = true)
    List<MaintenanceRequest> findByResidentIdWithPagination(
            @Param("residentId") UUID residentId,
            @Param("limit") int limit,
            @Param("offset") int offset);
}

