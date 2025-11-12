package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResidentCardRegistrationRepository extends JpaRepository<ResidentCardRegistration, UUID> {
    Optional<ResidentCardRegistration> findByIdAndUserId(UUID id, UUID userId);

    Optional<ResidentCardRegistration> findByVnpayTransactionRef(String vnpayTransactionRef);

    List<ResidentCardRegistration> findByResidentId(UUID residentId);

    List<ResidentCardRegistration> findByResidentIdAndUnitId(UUID residentId, UUID unitId);

    List<ResidentCardRegistration> findByUserId(UUID userId);

    List<ResidentCardRegistration> findByUserIdAndUnitId(UUID userId, UUID unitId);

    List<ResidentCardRegistration> findAllByOrderByCreatedAtDesc();
}
