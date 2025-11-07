package com.QhomeBase.financebillingservice.client;

import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseServiceClient {

    private final WebClient webClient;

    public ReadingCycleDto getReadingCycleById(UUID cycleId) {
        try {
            return webClient.get()
                    .uri("/api/reading-cycles/{cycleId}", cycleId)
                    .retrieve()
                    .bodyToMono(ReadingCycleDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Error fetching reading cycle {} from base-service: {}", cycleId, e.getMessage());
            throw new RuntimeException("Failed to fetch reading cycle from base-service: " + e.getMessage(), e);
        }
    }
}
