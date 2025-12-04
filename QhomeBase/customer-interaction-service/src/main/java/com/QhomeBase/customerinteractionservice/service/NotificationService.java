package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.notification.CreateNotificationRequest;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationDetailResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationPagedResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationWebSocketMessage;
import com.QhomeBase.customerinteractionservice.dto.notification.UpdateNotificationRequest;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import com.QhomeBase.customerinteractionservice.repository.NotificationRepository;
import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
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
    private final BaseServiceClient baseServiceClient;

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

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. scope = EXTERNAL (riêng tư cho cư dân)
        // 2. createdAt = today (không phải tương lai)
        if (shouldSendNotification(savedNotification)) {
            sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");
            notificationPushService.sendPushNotification(savedNotification);
            log.info("✅ [NotificationService] Sent realtime and FCM push notification for notification {} (EXTERNAL, createdAt = today)", savedNotification.getId());
        } else {
            log.info("⏭️ [NotificationService] Skipped sending notification for notification {} (scope={}, createdAt={})", 
                    savedNotification.getId(), savedNotification.getScope(), savedNotification.getCreatedAt());
        }

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

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. scope = EXTERNAL (riêng tư cho cư dân)
        // 2. createdAt = today (không phải tương lai)
        if (shouldSendNotification(updatedNotification)) {
            sendWebSocketNotification(updatedNotification, "NOTIFICATION_UPDATED");
            notificationPushService.sendPushNotification(updatedNotification);
            log.info("✅ [NotificationService] Sent realtime and FCM push notification for updated notification {} (EXTERNAL, createdAt = today)", updatedNotification.getId());
        } else {
            log.info("⏭️ [NotificationService] Skipped sending notification for updated notification {} (scope={}, createdAt={})", 
                    updatedNotification.getId(), updatedNotification.getScope(), updatedNotification.getCreatedAt());
        }

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

    /**
     * Kiểm tra xem có nên gửi notification (realtime + FCM push) không.
     * Chỉ gửi khi:
     * 1. scope = EXTERNAL (riêng tư cho cư dân)
     * 2. createdAt = today (không phải tương lai)
     */
    private boolean shouldSendNotification(Notification notification) {
        // Chỉ gửi cho notification có scope EXTERNAL (cho cư dân)
        if (notification.getScope() != NotificationScope.EXTERNAL) {
            return false;
        }
        
        // Chỉ gửi khi createdAt là hôm nay (không phải tương lai)
        Instant now = Instant.now();
        Instant createdAt = notification.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        
        // Kiểm tra createdAt không phải tương lai
        if (createdAt.isAfter(now)) {
            return false; // createdAt là tương lai, không gửi notification
        }
        
        // Kiểm tra createdAt là cùng ngày với hiện tại
        // So sánh ngày (không tính giờ)
        java.time.LocalDate createdAtDate = java.time.LocalDate.ofInstant(createdAt, java.time.ZoneId.systemDefault());
        java.time.LocalDate nowDate = java.time.LocalDate.ofInstant(now, java.time.ZoneId.systemDefault());
        
        return createdAtDate.equals(nowDate);
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
            NotificationType type = notification.getType();
            
            // Card-related notifications (CARD_FEE_REMINDER, CARD_APPROVED, CARD_REJECTED):
            // RIÊNG TƯ - chỉ hiển thị cho resident tạo thẻ
            // These notifications must have targetResidentId set
            if (type == NotificationType.CARD_FEE_REMINDER || 
                type == NotificationType.CARD_APPROVED || 
                type == NotificationType.CARD_REJECTED) {
                // Card notifications must have targetResidentId and match current resident
                if (notification.getTargetResidentId() == null) {
                    log.warn("⚠️ [NotificationService] Card notification {} missing targetResidentId, skipping", notification.getId());
                    return false; // Don't show card notifications without targetResidentId
                }
                return residentId != null && residentId.equals(notification.getTargetResidentId());
            }
            
            // For other notification types, use existing logic
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

    /**
     * Get residentId from userId by calling base-service
     * This ensures that notifications are filtered by the authenticated user's residentId
     */
    @Transactional(readOnly = true)
    public UUID getResidentIdFromUserId(UUID userId) {
        try {
            return baseServiceClient.getResidentIdByUserId(userId);
        } catch (Exception e) {
            log.error("❌ Error getting residentId from userId {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    private NotificationDetailResponse toDetailResponse(Notification notification) {
        return NotificationDetailResponse.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetBuildingId(notification.getTargetBuildingId())
                .targetResidentId(notification.getTargetResidentId())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void sendWebSocketNotification(Notification notification, String action) {
        try {
            log.info("🔔 [NotificationService] Sending WebSocket notification: action={}, notificationId={}, targetResidentId={}", 
                    action, notification.getId(), notification.getTargetResidentId());
            
            NotificationWebSocketMessage payload = NotificationWebSocketMessage.of(notification, action);
            
            // If notification has targetResidentId, only send to that specific resident
            // Don't broadcast to building/external channels to prevent other residents from receiving it
            if (notification.getTargetResidentId() != null) {
                String residentDestination = "/topic/notifications/resident/" + notification.getTargetResidentId();
                log.info("📤 [NotificationService] Sending WebSocket to resident-specific channel: {}", residentDestination);
                messagingTemplate.convertAndSend(residentDestination, payload);
                log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {} | ResidentId: {}", 
                        action, residentDestination, notification.getId(), notification.getTargetResidentId());
                log.info("✅ Notification sent successfully via WebSocket to resident {}", notification.getTargetResidentId());
                return;
            }

            // Gửi kênh tổng cho tất cả client quan tâm (only for non-resident-specific notifications)
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
        NotificationType type = request.getType();
        
        // Validate: All card-related notifications require residentId (private notifications)
        if (type == NotificationType.CARD_FEE_REMINDER || 
            type == NotificationType.CARD_APPROVED || 
            type == NotificationType.CARD_REJECTED) {
            if (request.getResidentId() == null) {
                log.error("❌ [NotificationService] Card notification (type={}) requires residentId, but it's null", type);
                throw new IllegalArgumentException("Card notifications must have residentId");
            }
        }
        
        // If residentId is provided, send directly to that resident (private notification)
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
            // IMPORTANT: When residentId is provided, always set targetResidentId and targetBuildingId = null
            // to ensure only the resident who created the request sees the notification (PRIVATE notification)
            // This applies to all request types: card registrations, cleaning requests, maintenance requests, etc.
            NotificationScope scope = NotificationScope.EXTERNAL;
            
            Notification notification = Notification.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(scope)
                    .targetResidentId(request.getResidentId()) // Set targetResidentId for specific resident (PRIVATE)
                    .targetBuildingId(null) // Always null when targeting specific resident (ensures PRIVATE notification)
                    .targetRole(null)
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .actionUrl(request.getActionUrl())
                    .iconUrl(request.getIconUrl())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            
            log.info("📡 [NotificationService] Preparing to send WebSocket notification: residentId={}, notificationId={}, type={}", 
                    request.getResidentId(), savedNotification.getId(), type);
            
            // Send WebSocket notification - will automatically route to resident-specific channel
            // since targetResidentId is set, it won't broadcast to building/external channels
            sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");
            
            // Push notification đã được gửi ở trên (dòng 440) qua sendPushNotificationToResident
            // Không cần gọi sendPushNotification nữa để tránh gửi trùng
            
            log.info("✅ Created internal notification for residentId: {} | Notification ID: {} | Type: {} | WebSocket sent to /topic/notifications/resident/{}", 
                    request.getResidentId(), savedNotification.getId(), type, request.getResidentId());
        } else {
            // Fallback to regular notification creation
            CreateNotificationRequest createRequest = CreateNotificationRequest.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(request.getTargetRole() != null 
                            ? NotificationScope.INTERNAL 
                            : NotificationScope.EXTERNAL)
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
