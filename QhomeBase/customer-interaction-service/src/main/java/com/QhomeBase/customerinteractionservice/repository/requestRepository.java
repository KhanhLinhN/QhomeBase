package com.QhomeBase.customerinteractionservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.QhomeBase.customerinteractionservice.model.Request;

@Repository
public interface requestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {

    @Query(value = "SELECT CAST(r.status AS TEXT) AS status, COUNT(r) AS count " +
            "FROM cs_service.requests r " +
            "WHERE (:projectCode IS NULL OR r.request_code = :projectCode) " +
            "AND (:title IS NULL OR r.title LIKE CONCAT('%', :title, '%')) " +
            "AND (:residentName IS NULL OR r.resident_name LIKE CONCAT('%', :residentName, '%')) " +
            "AND (:tenantId IS NULL OR r.tenant_id = CAST(:tenantId AS UUID)) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:priority IS NULL OR r.priority = :priority) " +
            "AND (CAST(:dateFrom AS DATE) IS NULL OR r.created_at >= CAST(:dateFrom AS DATE)) " +
            "AND (CAST(:dateTo AS DATE) IS NULL OR r.created_at < (CAST(:dateTo AS DATE) + interval '1 day')) " +
            "GROUP BY r.status", nativeQuery = true)
    List<StatusCountDTO> countRequestsByStatus(
            @Param("projectCode") String projectCode,
            @Param("title") String title,
            @Param("residentName") String residentName,
            @Param("tenantId") UUID tenantId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo
    );

}
