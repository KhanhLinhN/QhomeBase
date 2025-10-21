package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.security.JwtIssuer;
import com.QhomeBase.iamservice.security.UserPrincipal;
import com.QhomeBase.iamservice.security.AuthzService;
import com.QhomeBase.iamservice.dto.RolePermissionGrantRequest;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolesRepository;
import com.QhomeBase.iamservice.repository.TenantRolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import com.QhomeBase.iamservice.service.TenantRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private JwtIssuer jwtIssuer;
    
    @Autowired
    private AuthzService authzService;
    
    @Autowired
    private TenantRolePermissionRepository tenantRolePermissionRepository;
    
    @Autowired
    private RolesRepository rolesRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private TenantRolePermissionService tenantRolePermissionService;
    
    @Autowired
    private UserRolePermissionRepository userRolePermissionRepository;
    
    @Autowired
    private UserTenantRoleRepository userTenantRoleRepository;

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
            
            // Test authorization methods
            debugInfo.put("authorization", Map.of(
                "canViewTenantEmployees", authzService.canViewTenantEmployees(tenantId),
                "isAdmin", authzService.isAdmin(),
                "isTenantOwner", authzService.isTenantOwner(),
                "isTechnician", authzService.isTechnician(),
                "isSupporter", authzService.isSupporter(),
                "isAccount", authzService.isAccount()
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
            // Simulate the same logic as the actual endpoint
            boolean canView = authzService.canViewTenantEmployees(tenantId);
            result.put("canViewTenantEmployees", canView);
            
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
    
    @GetMapping("/debug-tenant-permissions/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Map<String, Object>> debugTenantPermissions(@PathVariable UUID tenantId) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Check tenant_role_permissions table - get roles first
            List<String> rolesInTenant = tenantRolePermissionRepository.findRolesWithPermissionsInTenant(tenantId);
            debug.put("roles_in_tenant", rolesInTenant);
            
            List<Map<String, Object>> permissions = new ArrayList<>();
            for (String role : rolesInTenant) {
                List<Object[]> rolePerms = tenantRolePermissionRepository.findPermissionsByTenantAndRole(tenantId, role);
                for (Object[] perm : rolePerms) {
                    permissions.add(Map.of(
                        "role", role,
                        "permission_code", perm[0],
                        "granted", perm[1]
                    ));
                }
            }
            debug.put("tenant_role_permissions_count", permissions.size());
            debug.put("tenant_role_permissions", permissions);
            
            // Check roles table
            List<Object[]> globalRoles = rolesRepository.findGlobalRoles();
            List<Map<String, Object>> roles = globalRoles.stream()
                .map(r -> Map.of("role", r[0], "description", r[1], "created_at", r[2]))
                .collect(Collectors.toList());
            debug.put("roles_count", roles.size());
            debug.put("roles", roles);
            
            // Check permissions table
            List<Permission> allPermissions = permissionRepository.findAll();
            debug.put("permissions_count", allPermissions.size());
            debug.put("permissions", allPermissions.stream()
                .map(p -> Map.of("code", p.getCode(), "description", p.getDescription() != null ? p.getDescription() : ""))
                .collect(Collectors.toList()));
            
            debug.put("status", "success");
            debug.put("tenantId", tenantId);
            
        } catch (Exception e) {
            debug.put("status", "error");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
    
    @PostMapping("/debug-grant-permission/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Map<String, Object>> debugGrantPermission(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> debug = new HashMap<>();
        
        try {
            String role = (String) request.get("role");
            @SuppressWarnings("unchecked")
            List<String> permissionCodes = (List<String>) request.get("permissionCodes");
            
            debug.put("input_role", role);
            debug.put("input_permissionCodes", permissionCodes);
            debug.put("tenantId", tenantId);
            
            // Check if role exists
            boolean roleExists = rolesRepository.isValidRole(role);
            debug.put("role_exists", roleExists);
            
            // Check if permissions exist
            List<Permission> allPermissions = permissionRepository.findAll();
            List<String> existingPermissions = allPermissions.stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toList());
            List<String> validPermissions = permissionCodes.stream()
                    .filter(existingPermissions::contains)
                    .collect(Collectors.toList());
            List<String> invalidPermissions = permissionCodes.stream()
                    .filter(p -> !existingPermissions.contains(p))
                    .collect(Collectors.toList());
            
            debug.put("valid_permissions", validPermissions);
            debug.put("invalid_permissions", invalidPermissions);
            
            // Try to grant permissions
            if (roleExists && !validPermissions.isEmpty()) {
                RolePermissionGrantRequest grantRequest = new RolePermissionGrantRequest();
                grantRequest.setRole(role);
                grantRequest.setPermissionCodes(validPermissions);
                
                tenantRolePermissionService.grantPermissionsToRole(tenantId, grantRequest, authentication);
                
                debug.put("grant_result", "success");
                
                // Check after grant
                List<Object[]> afterRolePerms = tenantRolePermissionRepository.findPermissionsByTenantAndRole(tenantId, role);
                List<Map<String, Object>> afterPermissions = afterRolePerms.stream()
                        .map(perm -> Map.of("permission_code", perm[0], "granted", perm[1]))
                        .collect(Collectors.toList());
                debug.put("after_grant_permissions_count", afterPermissions.size());
                debug.put("after_grant_permissions", afterPermissions);
                
            } else {
                debug.put("grant_result", "skipped - invalid role or permissions");
            }
            
            debug.put("status", "success");
            
        } catch (Exception e) {
            debug.put("status", "error");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
    
    @GetMapping("/debug-user-permissions/{userId}/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Map<String, Object>> debugUserPermissions(
            @PathVariable UUID userId, 
            @PathVariable UUID tenantId) {
        
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Test the same query used in AuthService.login()
            List<String> userPermissions = userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(userId, tenantId);
            
            debug.put("user_id", userId);
            debug.put("tenant_id", tenantId);
            debug.put("permissions_count", userPermissions.size());
            debug.put("permissions", userPermissions);
            
            // Check user roles in tenant
            List<String> userRoles = userTenantRoleRepository.findRolesInTenant(userId, tenantId);
            debug.put("user_roles_count", userRoles.size());
            debug.put("user_roles", userRoles);
            
            // Check tenant_role_permissions for this user's roles
            List<Map<String, Object>> tenantRolePermissions = new ArrayList<>();
            for (String role : userRoles) {
                List<Object[]> rolePerms = tenantRolePermissionRepository.findPermissionsByTenantAndRole(tenantId, role);
                for (Object[] perm : rolePerms) {
                    tenantRolePermissions.add(Map.of(
                        "role", role,
                        "permission_code", perm[0],
                        "granted", perm[1]
                    ));
                }
            }
            debug.put("tenant_role_permissions_count", tenantRolePermissions.size());
            debug.put("tenant_role_permissions", tenantRolePermissions);
            
            debug.put("status", "success");
            
        } catch (Exception e) {
            debug.put("status", "error");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
    
    @GetMapping("/debug-permission-breakdown/{userId}/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Map<String, Object>> debugPermissionBreakdown(
            @PathVariable UUID userId, 
            @PathVariable UUID tenantId) {
        
        Map<String, Object> debug = new HashMap<>();
        
        try {
            debug.put("user_id", userId);
            debug.put("tenant_id", tenantId);
            
            // Check user roles in tenant
            List<String> userRoles = userTenantRoleRepository.findRolesInTenant(userId, tenantId);
            debug.put("user_roles", userRoles);
            
            // Check global permissions for these roles
            List<String> globalPermissions = new ArrayList<>();
            for (String role : userRoles) {
                List<String> rolePerms = rolesRepository.findPermissionsByRole(role);
                globalPermissions.addAll(rolePerms);
            }
            debug.put("global_permissions_count", globalPermissions.size());
            debug.put("global_permissions", globalPermissions);
            
            // Check tenant-specific permissions for these roles
            List<Map<String, Object>> userTenantPermissions = new ArrayList<>();
            for (String role : userRoles) {
                List<Object[]> rolePerms = tenantRolePermissionRepository.findPermissionsByTenantAndRole(tenantId, role);
                for (Object[] perm : rolePerms) {
                    userTenantPermissions.add(Map.of(
                        "role", role,
                        "permission_code", perm[0],
                        "granted", perm[1]
                    ));
                }
            }
            debug.put("tenant_permissions_count", userTenantPermissions.size());
            debug.put("tenant_permissions", userTenantPermissions);
            
            // Check final combined permissions (same as login)
            List<String> finalPermissions = userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(userId, tenantId);
            debug.put("final_permissions_count", finalPermissions.size());
            debug.put("final_permissions", finalPermissions);
            
            debug.put("status", "success");
            
        } catch (Exception e) {
            debug.put("status", "error");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
}





