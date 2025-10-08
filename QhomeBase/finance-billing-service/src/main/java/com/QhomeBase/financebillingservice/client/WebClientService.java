package com.QhomeBase.financebillingservice.client;

import com.QhomeBase.financebillingservice.dto.BuildingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class WebClientService {


    private WebClient webClient;

    @EventListener(ApplicationReadyEvent.class)
    public void testWebClient() {
        webClient = WebClient.builder()
                .baseUrl("http://localhost:8081")
                .defaultHeader("Accept", "application/json")
                .build();
        log.info("=== TESTING WEBCLIENT ===");


        UUID tenantId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        try {
            List<BuildingDto> buildings = getAllBuildings(tenantId).collectList().block();
            log.info("Buildings found: {}", buildings.size());
            buildings.forEach(building -> log.info("Building: {} - {}", building.getCode(), building.getName()));
        } catch (Exception e) {
            log.error("Error testing WebClient: {}", e.getMessage());
        }
    }

    public Flux<BuildingDto> getAllBuildings(UUID tenantId) {
        return webClient.get()
                .uri("/api/buildings?tenantId={tenantId}", tenantId)
                .retrieve()
                .bodyToFlux(BuildingDto.class)
                .doOnNext(building -> log.info("Received building: {}", building.getName()))
                .doOnError(error -> log.error("Error fetching buildings: {}", error.getMessage()));
    }

    public Mono<BuildingDto> getBuildingById(UUID buildingId) {
        return webClient.get()
                .uri("/api/buildings/{id}", buildingId)
                .retrieve()
                .bodyToMono(BuildingDto.class);
    }
}