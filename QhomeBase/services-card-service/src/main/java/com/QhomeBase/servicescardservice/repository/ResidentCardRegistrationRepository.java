package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    List<ResidentCardRegistration> findByPaymentStatusAndUpdatedAtBefore(String paymentStatus, OffsetDateTime updatedAtBefore);

    @Query(value = """
        SELECT r.* FROM card.resident_card_registration r
        LEFT JOIN data.units u ON u.id = r.unit_id
        LEFT JOIN data.buildings b ON b.id = u.building_id
        WHERE (:buildingId IS NULL OR b.id = :buildingId)
          AND (:unitId IS NULL OR r.unit_id = :unitId)
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.approved_at DESC NULLS LAST, r.created_at DESC
        """, nativeQuery = true)
    List<ResidentCardRegistration> findApprovedCardsByBuildingAndUnit(
        @Param("buildingId") UUID buildingId,
        @Param("unitId") UUID unitId,
        @Param("status") String status
    );
}
