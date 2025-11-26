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
                    .onStatus(status -> status.isError(), response -> {
                        log.error("Error response from base-service when fetching reading cycles: {}", response.statusCode());
                        return response.createException().map(ex -> {
                            log.error("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
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
                    .onStatus(status -> status.isError(), response -> {
                        log.error("Error response from base-service when fetching reading cycle {}: {}", cycleId, response.statusCode());
                        return response.createException().map(ex -> {
                            log.error("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
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
    public List<ServiceInfo> getAllServices() {
        try {
            return webClient.get()
                    .uri("/api/services")
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        log.warn("Error response from base-service when fetching services: {}", response.statusCode());
                        return response.createException().map(ex -> {
                            log.warn("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
                    .bodyToFlux(ServiceInfo.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching services from base-service: {}", e.getMessage());
            return List.of();
        }
    }

    public HouseholdInfo getCurrentHouseholdByUnitId(UUID unitId) {
        try {
            return webClient.get()
                    .uri("/api/households/units/{unitId}/current", unitId)
                    .retrieve()
                    .bodyToMono(HouseholdInfo.class)
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching current household for unit {} from base-service: {}", unitId, e.getMessage());
            return null;
        }
    }

    public List<HouseholdMemberInfo> getActiveMembersByHouseholdId(UUID householdId) {
        try {
            return webClient.get()
                    .uri("/api/household-members/households/{householdId}", householdId)
                    .retrieve()
                    .bodyToFlux(HouseholdMemberInfo.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching active members for household {} from base-service: {}", householdId, e.getMessage());
            return List.of();
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
    public static class ServiceInfo {
        private UUID id;
        private String code;
        private String name;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class HouseholdInfo {
        private UUID id;
        private UUID unitId;
        private UUID primaryResidentId;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getUnitId() { return unitId; }
        public void setUnitId(UUID unitId) { this.unitId = unitId; }
        public UUID getPrimaryResidentId() { return primaryResidentId; }
        public void setPrimaryResidentId(UUID primaryResidentId) { this.primaryResidentId = primaryResidentId; }
    }

    public static class HouseholdMemberInfo {
        private UUID id;
        private UUID householdId;
        private UUID residentId;
        private String residentName;
        private Boolean isPrimary;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getHouseholdId() { return householdId; }
        public void setHouseholdId(UUID householdId) { this.householdId = householdId; }
        public UUID getResidentId() { return residentId; }
        public void setResidentId(UUID residentId) { this.residentId = residentId; }
        public String getResidentName() { return residentName; }
        public void setResidentName(String residentName) { this.residentName = residentName; }
        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    }
}
