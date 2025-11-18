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

    /**
     * Đếm số thẻ thang máy đã đăng ký cho unit (đã thanh toán thành công)
     * Chỉ đếm các registration đã được thanh toán (PAID) hoặc đã được approve (APPROVED)
     * Không đếm các registration chưa thanh toán (UNPAID, PAYMENT_PENDING) hoặc bị reject (REJECTED)
     * Dùng cho hiển thị số thẻ đã thanh toán thành công
     */
    @Query(value = "SELECT COUNT(*) FROM card.elevator_card_registration e " +
            "WHERE e.unit_id = :unitId " +
            "AND e.status != 'REJECTED' " +
            "AND (e.payment_status = 'PAID' OR e.status = 'APPROVED')", nativeQuery = true)
    long countElevatorCardsByUnitId(@Param("unitId") UUID unitId);

    /**
     * Đếm số thẻ thang máy đã đăng ký cho unit (bao gồm cả chưa thanh toán)
     * Đếm TẤT CẢ các registration trừ REJECTED và CANCELLED
     * Dùng cho validation khi đăng ký thẻ mới để đảm bảo không vượt quá giới hạn
     */
    @Query(value = "SELECT COUNT(*) FROM card.elevator_card_registration e " +
            "WHERE e.unit_id = :unitId " +
            "AND e.status != 'REJECTED' " +
            "AND e.status != 'CANCELLED'", nativeQuery = true)
    long countAllElevatorCardsByUnitId(@Param("unitId") UUID unitId);
}
