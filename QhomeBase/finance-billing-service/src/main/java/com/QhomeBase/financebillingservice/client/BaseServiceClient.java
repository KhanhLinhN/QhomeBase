package com.QhomeBase.financebillingservice.client;

import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseServiceClient {

    private final WebClient webClient;

    public List<ReadingCycleDto> getAllReadingCycles() {
        try {
            return webClient.get()
                    .uri("/api/reading-cycles")
                    .retrieve()
                    .bodyToFlux(ReadingCycleDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Error fetching reading cycles from base-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reading cycles from base-service: " + e.getMessage(), e);
        }
    }

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

    public UnitInfo getUnitById(UUID unitId) {
        try {
            return webClient.get()
                    .uri("/api/units/{unitId}", unitId)
                    .retrieve()
                    .bodyToMono(UnitInfo.class)
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching unit {} from base-service: {}", unitId, e.getMessage());
            return null;
        }
    }

    public static class UnitInfo {
        private UUID id;
        private UUID buildingId;
        private String code;
        private String name;
        private Integer floor;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getBuildingId() { return buildingId; }
        public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getFloor() { return floor; }
        public void setFloor(Integer floor) { this.floor = floor; }
    }
}
