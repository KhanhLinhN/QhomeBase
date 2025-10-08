package com.QhomeBase.iamservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantDeletionClient {
    private final WebClientService webClientService;
    public Mono<Map> createDeletion(UUID tenantId, String reason) {
        WebClient client = webClientService.getWebClient("http://localhost:8082", "qhome-base");
        return client.post()
                .uri("/api/tenant-deletions")
                .bodyValue(Map.of("tenantId", tenantId, "reason", reason))
                .retrieve()
                .bodyToMono(Map.class);
    }

    public Mono<Map> approveDeletion(UUID ticketId, String note) {
        WebClient client = webClientService.getWebClient("http://localhost:8082", "qhome-base");
        return client.post()
                .uri("/api/tenant-deletions/{id}/approve", ticketId)
                .bodyValue(Map.of("note", note))
                .retrieve()
                .bodyToMono(Map.class);
    }
}
