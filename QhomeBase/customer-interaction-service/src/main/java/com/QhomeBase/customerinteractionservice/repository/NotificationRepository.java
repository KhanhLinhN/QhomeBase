package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND (:targetRole IS NULL OR n.targetRole = :targetRole)
        AND (:targetBuildingId IS NULL OR n.targetBuildingId = :targetBuildingId)
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndTarget(
            @Param("scope") NotificationScope scope,
            @Param("targetRole") String targetRole,
            @Param("targetBuildingId") UUID targetBuildingId
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.targetBuildingId = :targetBuildingId
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndBuildingId(
            @Param("scope") NotificationScope scope,
            @Param("targetBuildingId") UUID targetBuildingId
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.targetRole = :targetRole
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndRole(
            @Param("scope") NotificationScope scope,
            @Param("targetRole") String targetRole
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeOrderByCreatedAtDesc(NotificationScope scope);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findAllActive();
    
    /**
     * Find notification by referenceId, type, and targetResidentId
     * Used to check if notification already exists to avoid duplicate FCM push
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.referenceId = :referenceId
        AND n.type = :type
        AND n.targetResidentId = :targetResidentId
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByReferenceIdAndTypeAndTargetResidentId(
            @Param("referenceId") UUID referenceId,
            @Param("type") com.QhomeBase.customerinteractionservice.model.NotificationType type,
            @Param("targetResidentId") UUID targetResidentId
    );
}














