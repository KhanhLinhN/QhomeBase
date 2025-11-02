package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.notification.*;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        validateNotificationScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

        Notification notification = Notification.builder()
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .scope(request.getScope())
                .targetRole(request.getTargetRole())
                .targetBuildingId(request.getTargetBuildingId())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .actionUrl(request.getActionUrl())
                .iconUrl(request.getIconUrl())
                .build();


        Notification savedNotification = notificationRepository.save(notification);

        sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");

        return toResponse(savedNotification);
    }

    public NotificationResponse updateNotification(UUID id, UpdateNotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        if (request.getTitle() != null) {
            notification.setTitle(request.getTitle());
        }
        if (request.getMessage() != null) {
            notification.setMessage(request.getMessage());
        }
        if (request.getActionUrl() != null) {
            notification.setActionUrl(request.getActionUrl());
        }
        if (request.getIconUrl() != null) {
            notification.setIconUrl(request.getIconUrl());
        }

        if (request.getScope() != null) {
            notification.setScope(request.getScope());

            if (request.getScope() == NotificationScope.INTERNAL) {
                if (request.getTargetRole() != null) {
                    notification.setTargetRole(request.getTargetRole());
                }
                notification.setTargetBuildingId(null);
            } else if (request.getScope() == NotificationScope.EXTERNAL) {
                notification.setTargetRole(null);
                if (request.getTargetBuildingId() != null) {
                    notification.setTargetBuildingId(request.getTargetBuildingId());
                }
            }

            validateNotificationScope(notification.getScope(), notification.getTargetRole(), notification.getTargetBuildingId());
        } else {
            if (request.getTargetRole() != null) {
                notification.setTargetRole(request.getTargetRole());
            }
            if (request.getTargetBuildingId() != null) {
                notification.setTargetBuildingId(request.getTargetBuildingId());
            }
        }

        Notification updatedNotification = notificationRepository.save(notification);

        sendWebSocketNotification(updatedNotification, "NOTIFICATION_UPDATED");

        return toResponse(updatedNotification);
    }

    public void deleteNotification(UUID id, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification already deleted");
        }

        notification.setDeletedAt(Instant.now());
        notification.setDeletedBy(userId);
        notificationRepository.save(notification);

        sendWebSocketNotification(notification, "NOTIFICATION_DELETED");
    }

    public List<NotificationResponse> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAllActive();
        
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
        
        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification not found with ID: " + id);
        }
        
        return toResponse(notification);
    }

    public List<NotificationResponse> getNotificationsForResident(UUID residentId, UUID buildingId) {
        List<Notification> notifications = notificationRepository.findByScopeAndBuildingId(
                NotificationScope.EXTERNAL,
                buildingId
        );

        return notifications.stream()
                .filter(n -> shouldShowNotificationToResident(n, buildingId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getNotificationsForRole(String role, UUID userId) {
        List<Notification> notifications = notificationRepository.findByScopeAndRole(
                NotificationScope.INTERNAL,
                role
        );

        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private boolean shouldShowNotificationToResident(Notification notification, UUID buildingId) {
        if (notification.getScope() == NotificationScope.INTERNAL) {
            return false;
        }

        if (notification.getScope() == NotificationScope.EXTERNAL) {
            if (notification.getTargetBuildingId() == null) {
                return true;
            }
            return buildingId != null && buildingId.equals(notification.getTargetBuildingId());
        }

        return false;
    }

    private void validateNotificationScope(NotificationScope scope, String targetRole, UUID targetBuildingId) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope is required");
        }

        if (scope == NotificationScope.INTERNAL) {
            if (targetRole == null || targetRole.isBlank()) {
                throw new IllegalArgumentException("INTERNAL notification must have target_role (use 'ALL' for all roles)");
            }
            if (targetBuildingId != null) {
                throw new IllegalArgumentException("INTERNAL notification cannot have target_building_id");
            }
        } else if (scope == NotificationScope.EXTERNAL) {
            if (targetRole != null && !targetRole.isBlank()) {
                throw new IllegalArgumentException("EXTERNAL notification cannot have target_role");
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetRole(notification.getTargetRole())
                .targetBuildingId(notification.getTargetBuildingId())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .actionUrl(notification.getActionUrl())
                .iconUrl(notification.getIconUrl())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    private void sendWebSocketNotification(Notification notification, String action) {
        try {
            String destination = "/topic/notifications";
            if (notification.getScope() == NotificationScope.EXTERNAL && notification.getTargetBuildingId() != null) {
                destination = "/topic/notifications/building/" + notification.getTargetBuildingId();
            } else if (notification.getScope() == NotificationScope.INTERNAL && notification.getTargetRole() != null) {
                destination = "/topic/notifications/role/" + notification.getTargetRole();
            }

            log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, destination, notification.getId());
            

            messagingTemplate.convertAndSend(destination, toResponse(notification));
            log.info("✅ Notification sent successfully via WebSocket");
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification", e);
        }
    }
}
