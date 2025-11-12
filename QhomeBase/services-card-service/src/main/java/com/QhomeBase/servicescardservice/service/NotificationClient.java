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
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.notification.base-url:http://localhost:8084}")
    private String notificationServiceBaseUrl;

    public void sendNotification(Map<String, Object> payload) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(notificationServiceBaseUrl)
                    .path("/api/notifications/internal")
                    .build()
                    .toUri();

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
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] Error sending notification", ex);
        }
    }

    public void sendResidentNotification(UUID residentId,
                                         String title,
                                         String body,
                                         Map<String, Object> data) {
        if (residentId == null) {
            log.warn("⚠️ [NotificationClient] residentId null, skip push");
            return;
        }
        sendNotification(Map.of(
                "target", Map.of(
                        "type", "RESIDENT",
                        "residentId", residentId.toString()
                ),
                "message", Map.of(
                        "title", title,
                        "body", body,
                        "data", data
                )
        ));
    }
}

