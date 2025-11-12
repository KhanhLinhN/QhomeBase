package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ElevatorCardRegistrationRepository extends JpaRepository<ElevatorCardRegistration, UUID> {
    Optional<ElevatorCardRegistration> findByIdAndUserId(UUID id, UUID userId);

    Optional<ElevatorCardRegistration> findByVnpayTransactionRef(String vnpayTransactionRef);
}


