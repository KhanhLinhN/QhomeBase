package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.CleaningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CleaningRequestRepository extends JpaRepository<CleaningRequest, UUID> {
}

