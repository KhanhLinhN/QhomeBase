package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegisterServiceRequestRepository extends JpaRepository<RegisterServiceRequest, UUID> {

    Optional<RegisterServiceRequest> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT r FROM RegisterServiceRequest r LEFT JOIN FETCH r.images WHERE r.id = :id AND r.userId = :userId")
    Optional<RegisterServiceRequest> findByIdAndUserIdWithImages(@Param("id") UUID id, @Param("userId") UUID userId);

    List<RegisterServiceRequest> findByUserId(UUID userId);

    List<RegisterServiceRequest> findByUserIdAndUnitId(UUID userId, UUID unitId);
}


