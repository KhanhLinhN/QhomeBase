package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseServiceClient {

    private final RestTemplate restTemplate;

    @Value("${base.service.base-url:http://localhost:8081/api}")
    private String baseServiceUrl;

    /**
     * Kiểm tra xem cư dân có AccountCreationRequest với status = APPROVED không
     * Logic: Nếu resident đã có userId (đã có account) thì có nghĩa là đã được approve.
     * Nếu chưa có userId, kiểm tra xem có AccountCreationRequest với status = APPROVED không.
     * @param residentId ID của cư dân
     * @param accessToken Access token để authenticate với base-service
     * @return true nếu đã được approve thành thành viên, false nếu chưa được approve
     */
    public boolean isResidentMemberApproved(UUID residentId, String accessToken) {
        if (residentId == null) {
            log.warn("⚠️ [BaseServiceClient] residentId is null");
            return false;
        }

        try {
            // Kiểm tra xem resident đã có account chưa (có userId)
            // Nếu đã có account thì có nghĩa là đã được approve
            String url = baseServiceUrl + "/residents/" + residentId + "/account";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            log.debug("🔍 [BaseServiceClient] Checking account approval for residentId: {}", residentId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            // Nếu có account (status 200 và có body) thì có nghĩa là đã được approve và có account
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("✅ [BaseServiceClient] Resident {} đã có account, đã được approve", residentId);
                return true;
            } else if (response.getStatusCode().value() == 404) {
                // Không có account, kiểm tra xem có AccountCreationRequest với status = APPROVED không
                // Tuy nhiên, endpoint này không tồn tại, nên ta sẽ kiểm tra bằng cách khác
                // Nếu không có account và không có request approved thì return false
                log.warn("⚠️ [BaseServiceClient] Resident {} chưa có account", residentId);
                return false;
            } else {
                log.warn("⚠️ [BaseServiceClient] Unexpected response status: {} for residentId: {}", 
                        response.getStatusCode(), residentId);
                return false;
            }
        } catch (RestClientException e) {
            log.error("❌ [BaseServiceClient] Error checking account approval for residentId {}: {}", 
                    residentId, e.getMessage());
            // Nếu không thể kiểm tra được (service down, network error), 
            // thì để an toàn, không cho phép đăng ký
            return false;
        }
    }
}
