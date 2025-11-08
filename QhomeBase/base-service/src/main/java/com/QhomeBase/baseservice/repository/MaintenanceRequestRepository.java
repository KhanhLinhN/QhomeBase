package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {
}

