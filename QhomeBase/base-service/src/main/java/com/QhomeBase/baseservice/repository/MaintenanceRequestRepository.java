package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {
    List<MaintenanceRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    List<MaintenanceRequest> findByStatusOrderByCreatedAtAsc(String status);
}

