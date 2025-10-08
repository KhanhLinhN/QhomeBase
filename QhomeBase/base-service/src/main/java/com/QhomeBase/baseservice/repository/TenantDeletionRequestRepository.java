package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.TenantDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantDeletionRequestRepository extends JpaRepository<TenantDeletionRequest, UUID> {
    Optional<TenantDeletionRequest> findById(UUID id);
}
