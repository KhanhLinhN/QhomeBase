package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.AccountCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountCreationRequestRepository extends JpaRepository<AccountCreationRequest, UUID> {

    List<AccountCreationRequest> findByStatus(AccountCreationRequest.RequestStatus status);

    List<AccountCreationRequest> findByResidentId(UUID residentId);

    List<AccountCreationRequest> findByRequestedBy(UUID requestedBy);

    @Query("SELECT r FROM AccountCreationRequest r WHERE r.residentId = :residentId AND r.status = 'PENDING'")
    Optional<AccountCreationRequest> findPendingByResidentId(@Param("residentId") UUID residentId);

    @Query("SELECT r FROM AccountCreationRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt DESC")
    List<AccountCreationRequest> findAllPendingRequests();
}

