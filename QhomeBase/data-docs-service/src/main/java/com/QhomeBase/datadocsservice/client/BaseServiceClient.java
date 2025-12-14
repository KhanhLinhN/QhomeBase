package com.QhomeBase.datadocsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.time.LocalDate;
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
        } catch (ResourceAccessException ex) {
            // Connection refused or service unavailable - log as WARN, not ERROR
            if (ex.getCause() instanceof ConnectException) {
                log.warn("⚠️ [BaseServiceClient] Base-service unavailable (connection refused) for unitId: {}. This is normal if base-service is not running.", unitId);
            } else {
                log.warn("⚠️ [BaseServiceClient] Network error connecting to base-service for unitId: {}", unitId, ex);
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
        } catch (ResourceAccessException ex) {
            // Connection refused or service unavailable - log as WARN, not ERROR
            if (ex.getCause() instanceof ConnectException) {
                log.warn("⚠️ [BaseServiceClient] Base-service unavailable (connection refused) for unitId: {}. This is normal if base-service is not running.", unitId);
            } else {
                log.warn("⚠️ [BaseServiceClient] Network error connecting to base-service for unitId: {}", unitId, ex);
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
        } catch (ResourceAccessException ex) {
            // Connection refused or service unavailable - log as WARN, not ERROR
            if (ex.getCause() instanceof ConnectException) {
                log.warn("⚠️ [BaseServiceClient] Base-service unavailable (connection refused) for unitId: {}. This is normal if base-service is not running.", unitId);
            } else {
                log.warn("⚠️ [BaseServiceClient] Network error connecting to base-service for unitId: {}", unitId, ex);
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

    /**
     * Create asset inspection for cancelled contract
     */
    public void createAssetInspection(UUID contractId, UUID unitId, LocalDate inspectionDate, LocalDate scheduledDate) {
        try {
            String url = baseServiceBaseUrl + "/api/asset-inspections";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contractId", contractId.toString());
            requestBody.put("unitId", unitId.toString());
            requestBody.put("inspectionDate", inspectionDate.toString());
            if (scheduledDate != null) {
                requestBody.put("scheduledDate", scheduledDate.toString());
            }
            // inspectorName and inspectorId can be null for now, will be assigned later
            requestBody.put("inspectorName", null);
            requestBody.put("inspectorId", null);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [BaseServiceClient] Created asset inspection for contract: {}, scheduledDate: {}", contractId, scheduledDate);
            } else {
                log.error("❌ [BaseServiceClient] Failed to create asset inspection. Status: {}", response.getStatusCode());
            }
        } catch (ResourceAccessException ex) {
            // Connection refused or service unavailable - log as WARN, not ERROR
            if (ex.getCause() instanceof ConnectException) {
                log.warn("⚠️ [BaseServiceClient] Base-service unavailable (connection refused) when creating asset inspection for contract: {}. This is normal if base-service is not running.", contractId);
            } else {
                log.warn("⚠️ [BaseServiceClient] Network error connecting to base-service when creating asset inspection for contract: {}", contractId, ex);
            }
            // Don't throw exception - allow contract cancellation to proceed even if inspection creation fails
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error creating asset inspection for contract: {}", contractId, ex);
            // Don't throw exception - allow contract cancellation to proceed even if inspection creation fails
        }
    }
}
