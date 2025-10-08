package com.QhomeBase.baseservice.client;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

public class WebSeverClient {
    private WebClient webClient;
    public Flux<UUID> getManagersTenantsIDs (UUID tenantID) {
        return webClient.get()
                .uri("/api/tenants/roles/{tenantId}/managers", tenantID)
                .retrieve()
                .bodyToFlux(UUID.class);

    }

}
