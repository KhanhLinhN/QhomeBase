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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class BaseServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.base.base-url:http://localhost:8081}")
    private String baseServiceBaseUrl;

    public BaseServiceClient() {
        this.restTemplate = new RestTemplate();
        // Configure timeout for inter-service calls only (does NOT affect Flutter client)
        // Flutter uses Dio/HTTP client directly, not this RestTemplate
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds connect timeout
        factory.setReadTimeout(10000); // 10 seconds read timeout (enough for most calls, but fails fast on deadlock)
        this.restTemplate.setRequestFactory(factory);
    }

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
     * Get all resident IDs in a unit (including household members)
     * Returns list of resident IDs who have accounts (userId != null)
     */
    public List<UUID> getAllResidentIdsByUnitId(UUID unitId) {
        try {
            // First, get household info
            String householdUrl = baseServiceBaseUrl + "/api/households/units/" + unitId + "/current";
            ResponseEntity<Map> householdResponse = restTemplate.getForEntity(householdUrl, Map.class);
            
            if (!householdResponse.getStatusCode().is2xxSuccessful() || householdResponse.getBody() == null) {
                log.warn("⚠️ [BaseServiceClient] Could not get household for unitId: {}", unitId);
                return java.util.Collections.emptyList();
            }
            
            Map<String, Object> household = householdResponse.getBody();
            Object householdIdObj = household.get("id");
            if (householdIdObj == null) {
                log.warn("⚠️ [BaseServiceClient] Household has no id for unitId: {}", unitId);
                return java.util.Collections.emptyList();
            }
            
            UUID householdId = householdIdObj instanceof UUID 
                    ? (UUID) householdIdObj 
                    : UUID.fromString(householdIdObj.toString());
            
            // Get all household members
            String membersUrl = baseServiceBaseUrl + "/api/household-members/households/" + householdId;
            ResponseEntity<List> membersResponse = restTemplate.getForEntity(membersUrl, List.class);
            
            if (!membersResponse.getStatusCode().is2xxSuccessful() || membersResponse.getBody() == null) {
                log.warn("⚠️ [BaseServiceClient] Could not get household members for householdId: {}", householdId);
                return java.util.Collections.emptyList();
            }
            
            List<Map<String, Object>> members = (List<Map<String, Object>>) membersResponse.getBody();
            List<UUID> residentIds = new java.util.ArrayList<>();
            
            // Get primary resident ID from household
            Object primaryResidentIdObj = household.get("primaryResidentId");
            if (primaryResidentIdObj != null) {
                UUID primaryResidentId = primaryResidentIdObj instanceof UUID 
                        ? (UUID) primaryResidentIdObj 
                        : UUID.fromString(primaryResidentIdObj.toString());
                residentIds.add(primaryResidentId);
            }
            
            // Add all other household members' resident IDs
            for (Map<String, Object> member : members) {
                Object residentIdObj = member.get("residentId");
                if (residentIdObj != null) {
                    UUID residentId = residentIdObj instanceof UUID 
                            ? (UUID) residentIdObj 
                            : UUID.fromString(residentIdObj.toString());
                    // Only add if not already in list (avoid duplicates)
                    if (!residentIds.contains(residentId)) {
                        residentIds.add(residentId);
                    }
                }
            }
            
            log.debug("✅ [BaseServiceClient] Found {} resident(s) in unit {}", residentIds.size(), unitId);
            return residentIds;
        } catch (ResourceAccessException ex) {
            // Connection refused or service unavailable - log as WARN, not ERROR
            if (ex.getCause() instanceof ConnectException) {
                log.warn("⚠️ [BaseServiceClient] Base-service unavailable (connection refused) for unitId: {}. This is normal if base-service is not running.", unitId);
            } else {
                log.warn("⚠️ [BaseServiceClient] Network error connecting to base-service for unitId: {}", unitId, ex);
            }
            return java.util.Collections.emptyList();
        } catch (Exception ex) {
            log.error("❌ [BaseServiceClient] Error getting all resident IDs for unitId: {}", unitId, ex);
            return java.util.Collections.emptyList();
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
     * Kiểm tra xem user có phải là OWNER (chủ căn hộ) của unit không
     * OWNER được định nghĩa là:
     * - household.kind == OWNER HOẶC TENANT (người mua hoặc người thuê căn hộ)
     * - VÀ user là primaryResidentId của household đó
     * @param userId ID của user
     * @param unitId ID của căn hộ
     * @param accessToken Access token để authenticate với base-service
     * @return true nếu user là OWNER của unit, false nếu không
     */
    public boolean isOwnerOfUnit(UUID userId, UUID unitId, String accessToken) {
        if (userId == null || unitId == null) {
            log.warn("⚠️ [BaseServiceClient] userId or unitId is null");
            return false;
        }

        try {
            // Lấy household info từ base-service
            String url = baseServiceBaseUrl + "/api/households/units/" + unitId + "/current";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            log.debug("🔍 [BaseServiceClient] Checking if user {} is OWNER of unit {}", userId, unitId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> household = response.getBody();
                
                // Kiểm tra household kind - OWNER hoặc TENANT đều được coi là chủ căn hộ
                Object kindObj = household.get("kind");
                if (kindObj == null) {
                    log.debug("⚠️ [BaseServiceClient] Household kind is null");
                    return false;
                }
                String kind = kindObj.toString();
                if (!"OWNER".equalsIgnoreCase(kind) && !"TENANT".equalsIgnoreCase(kind)) {
                    log.debug("⚠️ [BaseServiceClient] Household kind is not OWNER or TENANT: {}", kind);
                    return false;
                }
                
                // Kiểm tra primaryResidentId
                Object primaryResidentIdObj = household.get("primaryResidentId");
                if (primaryResidentIdObj == null) {
                    log.debug("⚠️ [BaseServiceClient] Household has no primaryResidentId");
                    return false;
                }
                
                // Lấy residentId từ userId
                String residentUrl = baseServiceBaseUrl + "/api/residents/by-user/" + userId;
                ResponseEntity<Map> residentResponse = restTemplate.exchange(
                        residentUrl,
                        org.springframework.http.HttpMethod.GET,
                        request,
                        Map.class
                );
                
                if (residentResponse.getStatusCode().is2xxSuccessful() && residentResponse.getBody() != null) {
                    Map<String, Object> resident = residentResponse.getBody();
                    Object residentIdObj = resident.get("id");
                    
                    if (residentIdObj != null) {
                        String residentId = residentIdObj.toString();
                        String primaryResidentId = primaryResidentIdObj.toString();
                        
                        boolean isOwner = residentId.equals(primaryResidentId);
                        log.debug("✅ [BaseServiceClient] User {} isOwner of unit {}: {}", userId, unitId, isOwner);
                        return isOwner;
                    }
                }
            }
            
            return false;
        } catch (ResourceAccessException e) {
            // Timeout or connection error - throw exception so caller can handle fallback
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("Read timed out") 
                    || errorMsg.contains("Connection timed out") || errorMsg.contains("Connection refused"))) {
                log.warn("⚠️ [BaseServiceClient] Timeout/connection error checking if user {} is OWNER of unit {}: {}", 
                        userId, unitId, errorMsg);
                throw new RuntimeException("Base-service timeout or unavailable: " + errorMsg, e);
            }
            log.error("❌ [BaseServiceClient] Error checking if user {} is OWNER of unit {}: {}", 
                    userId, unitId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ [BaseServiceClient] Error checking if user {} is OWNER of unit {}: {}", 
                    userId, unitId, e.getMessage());
            return false;
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
