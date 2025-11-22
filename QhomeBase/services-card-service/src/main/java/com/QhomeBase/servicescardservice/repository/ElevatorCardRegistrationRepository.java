package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElevatorCardRegistrationRepository extends JpaRepository<ElevatorCardRegistration, UUID> {
    Optional<ElevatorCardRegistration> findByIdAndUserId(UUID id, UUID userId);

    Optional<ElevatorCardRegistration> findByVnpayTransactionRef(String vnpayTransactionRef);

    List<ElevatorCardRegistration> findByResidentId(UUID residentId);

    List<ElevatorCardRegistration> findByResidentIdAndUnitId(UUID residentId, UUID unitId);

    List<ElevatorCardRegistration> findByUserId(UUID userId);

    List<ElevatorCardRegistration> findByUserIdAndUnitId(UUID userId, UUID unitId);

    List<ElevatorCardRegistration> findAllByOrderByCreatedAtDesc();

    List<ElevatorCardRegistration> findByPaymentStatusAndUpdatedAtBefore(String paymentStatus, OffsetDateTime updatedAtBefore);

    @Query(value = """
        SELECT e.* FROM card.elevator_card_registration e
        LEFT JOIN data.units u ON u.id = e.unit_id
        LEFT JOIN data.buildings b ON b.id = u.building_id
        WHERE (:buildingId IS NULL OR b.id = :buildingId)
          AND (:unitId IS NULL OR e.unit_id = :unitId)
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.approved_at DESC NULLS LAST, e.created_at DESC
        """, nativeQuery = true)
    List<ElevatorCardRegistration> findApprovedCardsByBuildingAndUnit(
        @Param("buildingId") UUID buildingId,
        @Param("unitId") UUID unitId,
        @Param("status") String status
    );
}
