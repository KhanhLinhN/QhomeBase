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

    /**
     * Kiểm tra xem đã có thẻ cư dân với CCCD này chưa (không tính các trạng thái REJECTED)
     */
    @Query("SELECT COUNT(r) > 0 FROM ResidentCardRegistration r " +
           "WHERE r.citizenId = :citizenId " +
           "AND r.status != 'REJECTED' " +
           "AND r.citizenId IS NOT NULL")
    boolean existsByCitizenId(@Param("citizenId") String citizenId);

    /**
     * Đếm số thẻ cư dân đã đăng ký cho unit (bao gồm cả chưa thanh toán)
     * Đếm TẤT CẢ các registration trừ REJECTED và CANCELLED
     * Dùng cho validation khi đăng ký thẻ mới để đảm bảo không vượt quá giới hạn
     */
    @Query("SELECT COUNT(r) FROM ResidentCardRegistration r " +
           "WHERE r.unitId = :unitId " +
           "AND r.status NOT IN :excludedStatuses")
    long countAllResidentCardsByUnitId(@Param("unitId") UUID unitId, 
                                       @Param("excludedStatuses") List<String> excludedStatuses);
}
