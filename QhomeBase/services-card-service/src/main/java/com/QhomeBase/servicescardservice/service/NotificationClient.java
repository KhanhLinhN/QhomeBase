package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    public void sendNotification(Map<String, Object> payload) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(notificationServiceBaseUrl)
                    .path("/api/notifications/internal")
                    .build()
                    .toUri();

            log.info("📤 [NotificationClient] Sending notification to: {} | Payload: {}", uri, payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Void> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("❌ [NotificationClient] Failed to push notification: status={}", response.getStatusCode());
            } else {
                log.info("✅ [NotificationClient] Notification sent successfully to notification service (will trigger WebSocket realtime)");
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] Error sending notification to notification service", ex);
            // Re-throw để caller biết có lỗi (optional, tùy vào yêu cầu)
            // throw new RuntimeException("Failed to send notification", ex);
        }
    }

    public void sendResidentNotification(UUID residentId,
                                         UUID buildingId,
                                         String type,
                                         String title,
                                         String message,
                                         UUID referenceId,
                                         String referenceType,
                                         Map<String, String> data) {
        // For public notifications (CARD_APPROVED, CARD_REJECTED), residentId can be null
        // but buildingId must be provided
        if (residentId == null && buildingId == null) {
            log.warn("⚠️ [NotificationClient] Both residentId and buildingId are null, skip push");
            return;
        }
        
        log.info("📨 [NotificationClient] Preparing notification: type={}, title={}, residentId={}, referenceId={}", 
                type, title, residentId, referenceId);
        
        Map<String, Object> payload = new HashMap<>();
        if (residentId != null) {
            payload.put("residentId", residentId.toString());
        }
        if (buildingId != null) {
            payload.put("buildingId", buildingId.toString());
        }
        payload.put("type", type != null ? type : "SYSTEM");
        payload.put("title", title);
        payload.put("message", message);
        if (referenceId != null) {
            payload.put("referenceId", referenceId.toString());
        }
        if (referenceType != null) {
            payload.put("referenceType", referenceType);
        }
        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }
        
        log.info("📤 [NotificationClient] Sending notification (will trigger FCM + WebSocket realtime): type={}, residentId={}", 
                type, residentId);
        sendNotification(payload);
    }
}

