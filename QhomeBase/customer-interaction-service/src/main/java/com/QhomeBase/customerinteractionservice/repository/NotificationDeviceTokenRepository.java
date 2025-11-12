package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.NotificationDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceToken, UUID> {

    Optional<NotificationDeviceToken> findByToken(String token);

    @Query("""
            select t from NotificationDeviceToken t
            where t.disabled = false
            """)
    List<NotificationDeviceToken> findAllActive();

    @Query("""
            select t from NotificationDeviceToken t
            where t.disabled = false
            and (t.buildingId is null or t.buildingId = :buildingId)
            """)
    List<NotificationDeviceToken> findForBuilding(@Param("buildingId") UUID buildingId);

    @Query("""
            select t from NotificationDeviceToken t
            where t.disabled = false
            and (t.role is null or lower(t.role) = lower(:role) or lower(t.role) = 'all')
            """)
    List<NotificationDeviceToken> findForRole(@Param("role") String role);

    @Modifying
    @Query("""
            update NotificationDeviceToken t
            set t.disabled = true, t.updatedAt = :updatedAt
            where t.token in :tokens
            """)
    void disableTokens(@Param("tokens") List<String> tokens,
                       @Param("updatedAt") Instant updatedAt);
}

