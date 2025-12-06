package com.QhomeBase.datadocsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class BaseServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.base.base-url:http://localhost:8081}")
    private String baseServiceBaseUrl;

    /**
     * Get primary residentId from unitId
     */
    public Optional<UUID> getPrimaryResidentIdByUnitId(UUID unitId) {
        try {
            String url = baseServiceBaseUrl + "/api/households/units/" + unitId + "/current";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object residentIdObj = response.getBody().get("primaryResidentId");
                if (residentIdObj != null) {
                    UUID residentId = residentIdObj instanceof UUID 
                            ? (UUID) residentIdObj 
                            : UUID.fromString(residentIdObj.toString());
                    return Optional.of(residentId);
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error getting primary residentId for unitId: {}", unitId, ex);
            return Optional.empty();
        }
    }

    /**
     * Get buildingId from unitId
     */
    public Optional<UUID> getBuildingIdByUnitId(UUID unitId) {
        try {
            String url = baseServiceBaseUrl + "/api/units/" + unitId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> building = (Map<String, Object>) response.getBody().get("building");
                if (building != null) {
                    Object buildingIdObj = building.get("id");
                    if (buildingIdObj != null) {
                        UUID buildingId = buildingIdObj instanceof UUID 
                                ? (UUID) buildingIdObj 
                                : UUID.fromString(buildingIdObj.toString());
                        return Optional.of(buildingId);
                    }
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error getting buildingId for unitId: {}", unitId, ex);
            return Optional.empty();
        }
    }

    /**
     * Get unit code from unitId
     */
    public Optional<String> getUnitCodeByUnitId(UUID unitId) {
        try {
            String url = baseServiceBaseUrl + "/api/units/" + unitId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object codeObj = response.getBody().get("code");
                if (codeObj != null) {
                    return Optional.of(codeObj.toString());
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error getting unit code for unitId: {}", unitId, ex);
            return Optional.empty();
        }
    }

    /**
     * Get residentId from userId
     * This is a fallback when household lookup fails
     */
    public Optional<UUID> getResidentIdByUserId(UUID userId) {
        try {
            // Call /api/users/me endpoint which returns user info including residentId
            String url = baseServiceBaseUrl + "/api/users/me";
            // Note: This endpoint requires authentication, so it may not work from service-to-service
            // Alternative: Query residents table directly via a new endpoint if available
            // For now, return empty and let caller handle
            log.warn("⚠️ [BaseServiceClient] getResidentIdByUserId not fully implemented. userId: {}", userId);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error getting residentId for userId: {}", userId, ex);
            return Optional.empty();
        }
    }
}
