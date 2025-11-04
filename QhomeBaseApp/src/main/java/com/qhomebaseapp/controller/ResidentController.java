package com.qhomebaseapp.controller;

import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${admin.api.base-url}")
    private String adminApiBaseUrl;

    @GetMapping("/me/uuid")
    public ResponseEntity<?> getResidentUuid(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "User ID not found"
            ));
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra xem user đã có residentId và buildingId chưa
        String residentId = user.getResidentId();
        String buildingId = user.getBuildingId();
        
        if (residentId != null && !residentId.isBlank() && 
            buildingId != null && !buildingId.isBlank()) {
            log.info("✅ User đã có residentId và buildingId trong database: residentId={}, buildingId={}", residentId, buildingId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "residentId", residentId,
                    "buildingId", buildingId,
                    "unitId", user.getUnitId() != null ? user.getUnitId() : "",
                    "source", "database"
            ));
        }

        String unitId = user.getUnitId();
        if (unitId == null || unitId.isBlank()) {
            log.warn("⚠️ User {} không có unitId", userId);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Bạn chưa được gán vào căn hộ nào. Vui lòng cập nhật unitId, residentId và buildingId trong profile.",
                    "residentId", "",
                    "buildingId", ""
            ));
        }

        try {
            // Thử các endpoint khác nhau để lấy residentId và buildingId từ unitId
            // Option 1: Thử endpoint /units/{unitId}/resident-info
            String url1 = String.format("%s/units/%s/resident-info", adminApiBaseUrl, unitId);
            log.info("🔍 [Option 1] Gọi admin API: {}", url1);
            
            // Option 2: Thử endpoint /units/{unitId} để lấy unit details
            String url2 = String.format("%s/units/%s", adminApiBaseUrl, unitId);
            log.info("🔍 [Option 2] Gọi admin API: {}", url2);
            
            // Option 3: Thử endpoint /residents/unit/{unitId}
            String url3 = String.format("%s/residents/unit/%s", adminApiBaseUrl, unitId);
            log.info("🔍 [Option 3] Gọi admin API: {}", url3);

            // Thử Option 1: /units/{unitId}/resident-info
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url1,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> data = response.getBody();

                if (data != null) {
                    String apiResidentId = (String) data.get("residentId");
                    String apiBuildingId = (String) data.get("buildingId");
                    
                    if (apiResidentId != null && !apiResidentId.isBlank() && 
                        apiBuildingId != null && !apiBuildingId.isBlank()) {
                        log.info("✅ [Option 1] Lấy được resident UUID từ admin API: residentId={}, buildingId={}", apiResidentId, apiBuildingId);
                        
                        // Lưu vào database để lần sau không cần gọi lại
                        user.setResidentId(apiResidentId);
                        user.setBuildingId(apiBuildingId);
                        userRepository.save(user);
                        log.info("💾 Đã lưu residentId và buildingId vào database");
                        
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "residentId", apiResidentId,
                                "buildingId", apiBuildingId,
                                "unitId", unitId,
                                "source", "admin_api"
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [Option 1] Không thành công: {}", e.getMessage());
            }
            
            // Thử Option 2: /units/{unitId}
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url2,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> data = response.getBody();

                if (data != null) {
                    String apiResidentId = (String) data.get("residentId");
                    String apiBuildingId = (String) data.get("buildingId");
                    if (apiBuildingId == null) {
                        apiBuildingId = (String) data.get("building"); // Try alternative field name
                    }
                    
                    if (apiResidentId != null && !apiResidentId.isBlank() && 
                        apiBuildingId != null && !apiBuildingId.isBlank()) {
                        log.info("✅ [Option 2] Lấy được resident UUID từ admin API: residentId={}, buildingId={}", apiResidentId, apiBuildingId);
                        
                        // Lưu vào database để lần sau không cần gọi lại
                        user.setResidentId(apiResidentId);
                        user.setBuildingId(apiBuildingId);
                        userRepository.save(user);
                        log.info("💾 Đã lưu residentId và buildingId vào database");
                        
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "residentId", apiResidentId,
                                "buildingId", apiBuildingId,
                                "unitId", unitId,
                                "source", "admin_api"
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [Option 2] Không thành công: {}", e.getMessage());
            }
            
            // Thử Option 3: /residents/unit/{unitId}
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url3,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> data = response.getBody();

                if (data != null) {
                    String apiResidentId = (String) data.get("residentId");
                    String apiBuildingId = (String) data.get("buildingId");
                    if (apiBuildingId == null) {
                        apiBuildingId = (String) data.get("building");
                    }
                    
                    if (apiResidentId != null && !apiResidentId.isBlank() && 
                        apiBuildingId != null && !apiBuildingId.isBlank()) {
                        log.info("✅ [Option 3] Lấy được resident UUID từ admin API: residentId={}, buildingId={}", apiResidentId, apiBuildingId);
                        
                        // Lưu vào database để lần sau không cần gọi lại
                        user.setResidentId(apiResidentId);
                        user.setBuildingId(apiBuildingId);
                        userRepository.save(user);
                        log.info("💾 Đã lưu residentId và buildingId vào database");
                        
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "residentId", apiResidentId,
                                "buildingId", apiBuildingId,
                                "unitId", unitId,
                                "source", "admin_api"
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [Option 3] Không thành công: {}", e.getMessage());
            }
            
            log.warn("⚠️ Tất cả các endpoint thử đều không thành công. Sử dụng fallback UUID.");
            
            // Fallback: Tạo UUID từ userId và unitId
            // Tạm thời sử dụng cách này cho đến khi admin API có endpoint
            log.info("⚠️ Không thể lấy từ admin API, sử dụng fallback: generate UUID từ userId và unitId");
            
            // Tạo deterministic UUID từ userId (format: 00000000-0000-0000-0000-000000000000)
            // Pad userId với 0 để đủ 8 ký tự
            String userIdHex = String.format("%08x", userId).toLowerCase();
            String residentIdFallback = userIdHex + "-0000-0000-0000-000000000000";
            
            // Tạo buildingId từ unitId (lấy 8 ký tự đầu của unitId và thêm suffix)
            // Nếu unitId là UUID hợp lệ, lấy phần đầu; nếu không, tạo từ hash
            String buildingIdFallback;
            if (unitId.length() >= 36 && unitId.contains("-")) {
                // unitId đã là UUID, lấy phần đầu làm prefix
                buildingIdFallback = unitId.substring(0, 8) + "-0000-0000-0000-000000000000";
            } else {
                // Tạo hash từ unitId
                int hash = unitId.hashCode();
                String hashHex = String.format("%08x", Math.abs(hash)).toLowerCase();
                buildingIdFallback = hashHex + "-0000-0000-0000-000000000000";
            }
            
            log.info("✅ Sử dụng fallback UUID: residentId={}, buildingId={}", residentIdFallback, buildingIdFallback);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "residentId", residentIdFallback,
                    "buildingId", buildingIdFallback,
                    "unitId", unitId,
                    "note", "Generated fallback UUID. Admin API endpoint chưa tồn tại."
            ));
            
        } catch (Exception e) {
            log.error("❌ Lỗi không mong đợi khi lấy resident UUID: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Không thể lấy thông tin cư dân: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                    "residentId", "",
                    "buildingId", "",
                    "unitId", unitId
            ));
        }
    }
}

