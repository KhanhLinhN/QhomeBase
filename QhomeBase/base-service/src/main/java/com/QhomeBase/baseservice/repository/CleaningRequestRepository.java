package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.CleaningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CleaningRequestRepository extends JpaRepository<CleaningRequest, UUID> {
    List<CleaningRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    List<CleaningRequest> findByStatusOrderByCreatedAtAsc(String status);
}

