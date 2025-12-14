package com.QhomeBase.datadocsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    public void sendContractRenewalReminderNotification(UUID residentId,
                                                         UUID buildingId,
                                                         UUID contractId,
                                                         String contractNumber,
                                                         int reminderNumber,
                                                         boolean isFinalReminder) {
        if (residentId == null) {
            log.warn("⚠️ [NotificationClient] residentId null, skip notification");
            return;
        }

        String title;
        String message;
        
        if (reminderNumber == 1) {
            title = "Nhắc nhở gia hạn hợp đồng";
            message = String.format("Hợp đồng %s của bạn sắp hết hạn trong vòng 1 tháng. Vui lòng gia hạn hoặc hủy hợp đồng.", contractNumber);
        } else if (reminderNumber == 2) {
            title = "Nhắc nhở gia hạn hợp đồng (Lần 2)";
            message = String.format("Hợp đồng %s của bạn sắp hết hạn. Vui lòng gia hạn hoặc hủy hợp đồng ngay.", contractNumber);
        } else {
            title = "Thông báo cuối cùng - Gia hạn hợp đồng";
            message = String.format("Hợp đồng %s của bạn sắp hết hạn. Bạn BẮT BUỘC phải gia hạn hoặc hủy hợp đồng ngay hôm nay.", contractNumber);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SYSTEM");
        payload.put("title", title);
        payload.put("message", message);
        payload.put("scope", "EXTERNAL");
        payload.put("targetResidentId", residentId.toString());
        if (buildingId != null) {
            payload.put("targetBuildingId", buildingId.toString());
        }
        payload.put("referenceId", contractId.toString());
        payload.put("referenceType", "CONTRACT_RENEWAL");
        payload.put("actionUrl", "/contracts/" + contractId + "/renewal");

        sendNotification(payload);
    }

    private void sendNotification(Map<String, Object> payload) {
        try {
            // Use /api/notifications/internal endpoint which allows inter-service calls without authentication
            URI uri = URI.create(notificationServiceBaseUrl + "/api/notifications/internal");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Convert payload to InternalNotificationRequest format
            // InternalNotificationRequest uses: type (NotificationType enum), title, message, residentId, buildingId, referenceId, referenceType, actionUrl
            Map<String, Object> internalPayload = new HashMap<>();
            // type should be NotificationType enum value (SYSTEM)
            internalPayload.put("type", "SYSTEM");
            internalPayload.put("title", payload.get("title"));
            internalPayload.put("message", payload.get("message"));
            // Map targetResidentId -> residentId
            if (payload.get("targetResidentId") != null) {
                internalPayload.put("residentId", payload.get("targetResidentId"));
            }
            // Map targetBuildingId -> buildingId
            if (payload.get("targetBuildingId") != null) {
                internalPayload.put("buildingId", payload.get("targetBuildingId"));
            }
            if (payload.get("referenceId") != null) {
                internalPayload.put("referenceId", payload.get("referenceId"));
            }
            if (payload.get("referenceType") != null) {
                internalPayload.put("referenceType", payload.get("referenceType"));
            }
            if (payload.get("actionUrl") != null) {
                internalPayload.put("actionUrl", payload.get("actionUrl"));
            }

            log.debug("📤 [NotificationClient] Sending internal notification to: {}", uri);
            log.debug("📤 [NotificationClient] Payload: {}", internalPayload);

            ResponseEntity<Void> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(internalPayload, headers),
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("❌ [NotificationClient] Failed to send notification: status={}", response.getStatusCode());
            } else {
                log.info("✅ [NotificationClient] Contract renewal reminder notification sent successfully");
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] Error sending notification", ex);
        }
    }
}
