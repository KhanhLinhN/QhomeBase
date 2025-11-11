package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.model.Notification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private static final int FCM_MAX_TOKENS_PER_REQUEST = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationDeviceTokenService deviceTokenService;

    public void sendPushNotification(Notification notification) {
        List<String> tokens = deviceTokenService.resolveTokensForNotification(notification);
        if (tokens.isEmpty()) {
            log.info("ℹ️ No device tokens found for notification {}", notification.getId());
            return;
        }

        log.info("🔔 Sending FCM notification {} to {} tokens", notification.getId(), tokens.size());

        Map<String, String> dataPayload = buildDataPayload(notification);
        com.google.firebase.messaging.Notification firebaseNotification =
                com.google.firebase.messaging.Notification.builder()
                        .setTitle(Optional.ofNullable(notification.getTitle()).orElse("Thông báo mới"))
                        .setBody(Optional.ofNullable(notification.getMessage()).orElse(""))
                        .build();

        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += FCM_MAX_TOKENS_PER_REQUEST) {
            int end = Math.min(i + FCM_MAX_TOKENS_PER_REQUEST, tokens.size());
            List<String> batch = tokens.subList(i, end);

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(firebaseNotification)
                    .putAllData(dataPayload)
                    .addAllTokens(batch)
                    .build();

            try {
                var response = firebaseMessaging.sendEachForMulticast(message);
                log.info("✅ FCM send result: success={}, failure={} for batch {}",
                        response.getSuccessCount(), response.getFailureCount(), (i / FCM_MAX_TOKENS_PER_REQUEST) + 1);

                List<SendResponse> sendResponses = response.getResponses();
                for (int j = 0; j < sendResponses.size(); j++) {
                    SendResponse sendResponse = sendResponses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String errorCode = Optional.ofNullable(sendResponse.getException())
                                .map(ex -> ex.getMessagingErrorCode() != null
                                        ? ex.getMessagingErrorCode().name()
                                        : ex.getMessage())
                                .orElse("UNKNOWN");

                        log.warn("⚠️ Failed to send token {} due to {}",
                                batch.get(j), errorCode);

                        if ("UNREGISTERED".equalsIgnoreCase(errorCode)
                                || "INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                            invalidTokens.add(batch.get(j));
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("❌ Error sending FCM notification batch", e);
            }
        }

        if (!invalidTokens.isEmpty()) {
            deviceTokenService.markTokensAsInvalid(invalidTokens);
        }
    }

    private Map<String, String> buildDataPayload(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getType() != null ? notification.getType().name() : "SYSTEM");
        Optional.ofNullable(notification.getReferenceId())
                .map(UUID::toString)
                .ifPresent(id -> data.put("referenceId", id));
        Optional.ofNullable(notification.getReferenceType())
                .ifPresent(type -> data.put("referenceType", type));

        Optional.ofNullable(notification.getScope())
                .ifPresent(scope -> data.put("scope", scope.name()));

        Optional.ofNullable(notification.getTargetBuildingId())
                .map(UUID::toString)
                .ifPresent(id -> data.put("targetBuildingId", id));

        Optional.ofNullable(notification.getTargetRole())
                .ifPresent(role -> data.put("targetRole", role));

        return data;
    }
}

