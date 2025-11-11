package com.QhomeBase.customerinteractionservice.client;

import com.QhomeBase.customerinteractionservice.client.dto.ResidentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseServiceClient {

    private final WebClient baseServiceWebClient;

    public ResidentResponse getResidentById(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/{residentId}", residentId)
                    .retrieve()
                    .bodyToMono(ResidentResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Resident not found for id {}", residentId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch resident by id {}: {}", residentId, e.getMessage());
            return null;
        }
    }

    public ResidentResponse getResidentByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/by-user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(ResidentResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Resident not found for user {}", userId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch resident by user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public String fetchResidentNameById(UUID residentId) {
        ResidentResponse resident = getResidentById(residentId);
        return resident != null ? resident.fullName() : null;
    }
}

