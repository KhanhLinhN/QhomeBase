package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.security.JwtIssuer;
import com.QhomeBase.iamservice.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtIssuer jwtIssuer;

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
            // Tạo test token với dữ liệu từ request
            String token = jwtIssuer.issueForService(
                request.getUid() != null ? request.getUid() : UUID.randomUUID(),
                request.getUsername() != null ? request.getUsername() : "testuser",
                request.getTenantId() != null ? request.getTenantId() : UUID.randomUUID(),
                request.getRoles() != null ? request.getRoles() : List.of("USER"),
                request.getPermissions() != null ? request.getPermissions() : List.of("READ"),
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

    // DTOs
    public static class TokenRequest {
        private UUID uid;
        private String username;
        private UUID tenantId;
        private List<String> roles;
        private List<String> permissions;

        // Getters and setters
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

        // Getters and setters
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

        // Getters and setters
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

