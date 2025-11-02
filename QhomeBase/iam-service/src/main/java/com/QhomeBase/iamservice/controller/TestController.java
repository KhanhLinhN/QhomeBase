package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.security.JwtIssuer;
import com.QhomeBase.iamservice.security.UserPrincipal;
import com.QhomeBase.iamservice.security.AuthzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtIssuer jwtIssuer;
    
    @Autowired
    private AuthzService authzService;

    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("This is a public endpoint - no JWT required");
    }

    @GetMapping("/protected")
    public ResponseEntity<String> protectedEndpoint(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok("Hello " + principal.username() + "! This is a protected endpoint.");
        }
        return ResponseEntity.ok("This is a protected endpoint - JWT required");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok("Hello Admin " + principal.username() + "! This is an admin-only endpoint.");
    }

    @PostMapping("/generate-token")
    public ResponseEntity<TokenResponse> generateTestToken(@RequestBody TokenRequest request) {
        try {
            String token = jwtIssuer.issueForService(
                request.getUid() != null ? request.getUid() : UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                request.getUsername() != null ? request.getUsername() : "testuser",
                request.getTenantId() != null ? request.getTenantId() : UUID.fromString("2b5b2af5-9431-4649-8144-35830d866826"),
                request.getRoles() != null ? request.getRoles() : List.of("tenant_manager"),
                request.getPermissions() != null ? request.getPermissions() : List.of("base.building.create"),
                "qhome-base"
            );
            
            return ResponseEntity.ok(new TokenResponse(token, "Token generated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new TokenResponse(null, "Error generating token: " + e.getMessage()));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<UserInfoResponse> getUserInfo(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return ResponseEntity.ok(new UserInfoResponse(
                principal.uid(),
                principal.username(),
                principal.tenant(),
                principal.roles(),
                principal.perms()
            ));
        }
        return ResponseEntity.badRequest().body(null);
    }

    @GetMapping("/debug-employee-roles/{tenantId}")
    public ResponseEntity<Map<String, Object>> debugEmployeeRoles(
            @PathVariable UUID tenantId,
            Authentication authentication) {
        
        Map<String, Object> debugInfo = new HashMap<>();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            
            debugInfo.put("user", Map.of(
                "userId", principal.uid(),
                "username", principal.username(),
                "tenantId", principal.tenant(),
                "roles", principal.roles(),
                "permissions", principal.perms()
            ));
            
            debugInfo.put("requestTenantId", tenantId);
            debugInfo.put("sameTenant", principal.tenant() != null && principal.tenant().equals(tenantId));
            
            debugInfo.put("authorization", Map.of(
                "canViewAllUsers", authzService.canViewAllUsers(),
                "isAdmin", authzService.isAdmin(),
                "isTenantOwner", authzService.isTenantOwner(),
                "isTechnician", authzService.isTechnician(),
                "isSupporter", authzService.isSupporter(),
                "isAccountant", authzService.isAccountant()
            ));
            
        } else {
            debugInfo.put("error", "No valid authentication found");
        }
        
        return ResponseEntity.ok(debugInfo);
    }

    @GetMapping("/test-available-roles/{tenantId}")
    public ResponseEntity<Map<String, Object>> testAvailableRoles(
            @PathVariable UUID tenantId,
            Authentication authentication) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean canView = authzService.canViewAllUsers();
            result.put("canViewAllUsers", canView);
            
            if (canView) {
                result.put("status", "SUCCESS - Would return available roles");
                result.put("message", "Authorization passed, service would be called");
            } else {
                result.put("status", "FORBIDDEN - Authorization failed");
                result.put("message", "User does not have permission to view tenant employees");
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }


    public static class TokenRequest {
        private UUID uid;
        private String username;
        private UUID tenantId;
        private List<String> roles;
        private List<String> permissions;


        public UUID getUid() { return uid; }
        public void setUid(UUID uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }

    public static class TokenResponse {
        private String token;
        private String message;

        public TokenResponse(String token, String message) {
            this.token = token;
            this.message = message;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class UserInfoResponse {
        private UUID uid;
        private String username;
        private UUID tenant;
        private List<String> roles;
        private List<String> permissions;

        public UserInfoResponse(UUID uid, String username, UUID tenant, List<String> roles, List<String> permissions) {
            this.uid = uid;
            this.username = username;
            this.tenant = tenant;
            this.roles = roles;
            this.permissions = permissions;
        }

        public UUID getUid() { return uid; }
        public void setUid(UUID uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public UUID getTenant() { return tenant; }
        public void setTenant(UUID tenant) { this.tenant = tenant; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }
    
}





