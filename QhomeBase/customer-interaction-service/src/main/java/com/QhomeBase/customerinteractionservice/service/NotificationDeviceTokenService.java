package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.notification.DeviceTokenResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.RegisterDeviceTokenRequest;
import com.QhomeBase.customerinteractionservice.model.NotificationDeviceToken;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.repository.NotificationDeviceTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeviceTokenService {

    private final NotificationDeviceTokenRepository repository;

    @Transactional
    public DeviceTokenResponse registerToken(RegisterDeviceTokenRequest request) {
        Optional<NotificationDeviceToken> existing = repository.findByToken(request.getToken());
        NotificationDeviceToken entity = existing.orElseGet(NotificationDeviceToken::new);

        entity.setToken(request.getToken());
        entity.setUserId(request.getUserId());
        entity.setResidentId(request.getResidentId());
        entity.setBuildingId(request.getBuildingId());
        entity.setRole(request.getRole());
        entity.setPlatform(request.getPlatform());
        entity.setAppVersion(request.getAppVersion());
        entity.setDisabled(false);
        entity.setLastSeenAt(Instant.now());

        NotificationDeviceToken saved = repository.save(entity);
        log.info("✅ Registered device token {} for user {}", saved.getToken(), saved.getUserId());

        return DeviceTokenResponse.builder()
                .id(saved.getId())
                .token(saved.getToken())
                .platform(saved.getPlatform())
                .appVersion(saved.getAppVersion())
                .lastSeenAt(saved.getLastSeenAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public void markTokensAsInvalid(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        repository.disableTokens(tokens, Instant.now());
        log.warn("⚠️ Disabled {} invalid FCM tokens", tokens.size());
    }

    @Transactional
    public void updateLastSeen(String token) {
        repository.findByToken(token).ifPresent(entity -> {
            entity.setLastSeenAt(Instant.now());
            repository.save(entity);
        });
    }

    @Transactional
    public void removeToken(String token) {
        repository.findByToken(token).ifPresent(repository::delete);
    }

    @Transactional
    public List<String> resolveTokensForNotification(Notification notification) {
        List<NotificationDeviceToken> candidates = new ArrayList<>();

        if (notification.getScope() == NotificationScope.EXTERNAL) {
            if (notification.getTargetBuildingId() != null) {
                candidates.addAll(repository.findForBuilding(notification.getTargetBuildingId()));
            } else {
                candidates.addAll(repository.findAllActive());
            }
        } else if (notification.getScope() == NotificationScope.INTERNAL) {
            String role = notification.getTargetRole() != null ? notification.getTargetRole() : "ALL";
            candidates.addAll(repository.findForRole(role));
        } else {
            candidates.addAll(repository.findAllActive());
        }

        return candidates.stream()
                .filter(token -> !token.isDisabled())
                .map(NotificationDeviceToken::getToken)
                .distinct()
                .collect(Collectors.toList());
    }
}

