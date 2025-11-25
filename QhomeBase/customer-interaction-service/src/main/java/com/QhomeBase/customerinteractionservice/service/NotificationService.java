package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.notification.CreateNotificationRequest;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationDetailResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationPagedResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationWebSocketMessage;
import com.QhomeBase.customerinteractionservice.dto.notification.UpdateNotificationRequest;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPushService notificationPushService;
    private final NotificationDeviceTokenService deviceTokenService;

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
        notificationPushService.sendPushNotification(savedNotification);

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
        notificationPushService.sendPushNotification(updatedNotification);

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

    public NotificationDetailResponse getNotificationDetailById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
        
        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification not found with ID: " + id);
        }
        
        return toDetailResponse(notification);
    }

    public List<NotificationResponse> getNotificationsForResident(UUID residentId, UUID buildingId) {
        NotificationPagedResponse pagedResponse = getNotificationsForResidentPaged(residentId, buildingId, 0, 7);
        return pagedResponse.getContent();
    }

    public NotificationPagedResponse getNotificationsForResidentPaged(UUID residentId, UUID buildingId, int page, int size) {
        List<Notification> allNotifications = notificationRepository.findByScopeOrderByCreatedAtDesc(
                NotificationScope.EXTERNAL
        );

        // Filter and sort by createdAt DESC (newest first)
        List<NotificationResponse> filteredAndSorted = allNotifications.stream()
                .filter(n -> shouldShowNotificationToResident(n, residentId, buildingId))
                .sorted((n1, n2) -> {
                    // Sort by createdAt DESC (newest first, from largest to smallest date)
                    Instant createdAt1 = n1.getCreatedAt();
                    Instant createdAt2 = n2.getCreatedAt();
                    
                    if (createdAt1 != null && createdAt2 != null) {
                        return createdAt2.compareTo(createdAt1); // Sort DESC (newest first)
                    }
                    if (createdAt1 != null) return -1;
                    if (createdAt2 != null) return 1;
                    return 0;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Calculate pagination
        long totalElements = filteredAndSorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Ensure page is within valid range
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filteredAndSorted.size());
        List<NotificationResponse> pagedContent = start < filteredAndSorted.size()
                ? filteredAndSorted.subList(start, end)
                : new java.util.ArrayList<>();

        return NotificationPagedResponse.builder()
                .content(pagedContent)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .build();
    }

    /**
     * Get total count of notifications for resident (all pages, not just current page)
     * This is used for displaying unread count on home screen
     */
    public long getNotificationsCountForResident(UUID residentId, UUID buildingId) {
        List<Notification> allNotifications = notificationRepository.findByScopeOrderByCreatedAtDesc(
                NotificationScope.EXTERNAL
        );

        return allNotifications.stream()
                .filter(n -> shouldShowNotificationToResident(n, residentId, buildingId))
                .count();
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

    private boolean shouldShowNotificationToResident(Notification notification, UUID residentId, UUID buildingId) {
        if (notification.getScope() == NotificationScope.INTERNAL) {
            return false;
        }

        if (notification.getScope() == NotificationScope.EXTERNAL) {
            // If notification has targetResidentId, only show to that specific resident
            if (notification.getTargetResidentId() != null) {
                return residentId != null && residentId.equals(notification.getTargetResidentId());
            }
            
            // Otherwise, use building-based filtering (for notifications to all residents in building or all buildings)
            if (notification.getTargetBuildingId() == null) {
                return true; // Show to all buildings
            }
            return buildingId != null && buildingId.equals(notification.getTargetBuildingId());
        }

        return false;
    }

    private NotificationDetailResponse toDetailResponse(Notification notification) {
        return NotificationDetailResponse.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetBuildingId(notification.getTargetBuildingId())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void sendWebSocketNotification(Notification notification, String action) {
        NotificationWebSocketMessage payload = NotificationWebSocketMessage.of(notification, action);
        try {
            // Gửi kênh tổng cho tất cả client quan tâm
            messagingTemplate.convertAndSend("/topic/notifications", payload);
            log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, "/topic/notifications", notification.getId());

            if (notification.getScope() == NotificationScope.EXTERNAL) {
                if (notification.getTargetBuildingId() != null) {
                    String destination = "/topic/notifications/building/" + notification.getTargetBuildingId();
                    messagingTemplate.convertAndSend(destination, payload);
                    log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, destination, notification.getId());
                } else {
                    messagingTemplate.convertAndSend("/topic/notifications/external", payload);
                    log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, "/topic/notifications/external", notification.getId());
                }
            } else if (notification.getScope() == NotificationScope.INTERNAL) {
                if (notification.getTargetRole() != null && !notification.getTargetRole().isBlank()) {
                    String destination = "/topic/notifications/role/" + notification.getTargetRole();
                    messagingTemplate.convertAndSend(destination, payload);
                    log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, destination, notification.getId());
                } else {
                    messagingTemplate.convertAndSend("/topic/notifications/internal", payload);
                    log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, "/topic/notifications/internal", notification.getId());
                }
            }

            log.info("✅ Notification sent successfully via WebSocket");
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification", e);
        }
    }
    
    private void sendWebSocketNotificationToResident(Notification notification, UUID residentId, String action) {
        NotificationWebSocketMessage payload = NotificationWebSocketMessage.of(notification, action);
        try {
            // Send to resident-specific channel first (most specific)
            String residentDestination = "/topic/notifications/resident/" + residentId;
            messagingTemplate.convertAndSend(residentDestination, payload);
            log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {} | ResidentId: {}", 
                    action, residentDestination, notification.getId(), residentId);
            
            // Also send to building channel if applicable (for backward compatibility)
            if (notification.getScope() == NotificationScope.EXTERNAL && notification.getTargetBuildingId() != null) {
                String buildingDestination = "/topic/notifications/building/" + notification.getTargetBuildingId();
                messagingTemplate.convertAndSend(buildingDestination, payload);
                log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, buildingDestination, notification.getId());
            }
            
            log.info("✅ Notification sent successfully via WebSocket to resident {}", residentId);
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification to resident {}", residentId, e);
        }
    }

    public void createInternalNotification(com.QhomeBase.customerinteractionservice.dto.notification.InternalNotificationRequest request) {
        // If residentId is provided, send directly to that resident
        if (request.getResidentId() != null) {
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("type", request.getType() != null ? request.getType().name() : "SYSTEM");
            if (request.getReferenceId() != null) {
                dataPayload.put("referenceId", request.getReferenceId().toString());
            }
            if (request.getReferenceType() != null) {
                dataPayload.put("referenceType", request.getReferenceType());
            }
            if (request.getData() != null) {
                dataPayload.putAll(request.getData());
            }

            // Send push notification directly to resident
            notificationPushService.sendPushNotificationToResident(
                    request.getResidentId(),
                    request.getTitle(),
                    request.getMessage(),
                    dataPayload
            );

            // Also save to DB with scope EXTERNAL and targetResidentId for specific resident
            NotificationScope scope = NotificationScope.EXTERNAL;
            
            Notification notification = Notification.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(scope)
                    .targetResidentId(request.getResidentId()) // Set targetResidentId for specific resident
                    .targetBuildingId(null) // Don't set targetBuildingId when targeting specific resident
                    .targetRole(null)
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .actionUrl(request.getActionUrl())
                    .iconUrl(request.getIconUrl())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            
            // Send WebSocket notification to resident-specific channel for real-time update
            sendWebSocketNotificationToResident(savedNotification, request.getResidentId(), "NOTIFICATION_CREATED");
            
            // Also send to general channels (building/external) for clients that subscribe to those channels
            sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");
            
            log.info("✅ Created internal notification for residentId: {} | Notification ID: {}", 
                    request.getResidentId(), savedNotification.getId());
        } else {
            // Fallback to regular notification creation
            CreateNotificationRequest createRequest = CreateNotificationRequest.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(request.getBuildingId() != null 
                            ? NotificationScope.EXTERNAL 
                            : (request.getTargetRole() != null 
                                    ? NotificationScope.INTERNAL 
                                    : NotificationScope.EXTERNAL))
                    .targetBuildingId(request.getBuildingId())
                    .targetRole(request.getTargetRole())
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .actionUrl(request.getActionUrl())
                    .iconUrl(request.getIconUrl())
                    .build();
            
            createNotification(createRequest);
        }
    }
}
