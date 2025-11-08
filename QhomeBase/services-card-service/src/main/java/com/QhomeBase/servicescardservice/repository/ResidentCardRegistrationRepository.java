package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResidentCardRegistrationRepository extends JpaRepository<ResidentCardRegistration, UUID> {
    Optional<ResidentCardRegistration> findByIdAndUserId(UUID id, UUID userId);
    Optional<ResidentCardRegistration> findByVnpayTransactionRef(String vnpayTransactionRef);
}


