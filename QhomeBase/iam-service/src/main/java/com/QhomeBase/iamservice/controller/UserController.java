package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserRolePermissionRepository userRolePermissionRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserInfoDto> getUserInfo(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    List<UUID> tenantIds = authService.getUserTenants(userId);
                    UUID primaryTenant = tenantIds.isEmpty() ? null : tenantIds.get(0);
                    List<String> roles = primaryTenant != null ? 
                        authService.getUserRolesInTenant(userId, primaryTenant) : List.of();

                    List<String> permissions = primaryTenant != null ? 
                        userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(userId, primaryTenant) : List.of();

                    UserInfoDto userInfo = new UserInfoDto(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getEmail(),
                            primaryTenant != null ? primaryTenant.toString() : null,
                            null,
                            roles,
                            permissions
                    );
                    return ResponseEntity.ok(userInfo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/tenants")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<List<UUID>> getUserTenants(@PathVariable UUID userId) {
        try {
            List<UUID> tenants = authService.getUserTenants(userId);
            return ResponseEntity.ok(tenants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/tenant/{tenantId}/roles")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<List<String>> getUserRolesInTenant(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        try {
            List<String> roles = authService.getUserRolesInTenant(userId, tenantId);
            return ResponseEntity.ok(roles);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/tenant/{tenantId}/validate")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<Boolean> validateUserAccess(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        boolean hasAccess = authService.validateUserAccess(userId, tenantId);
        return ResponseEntity.ok(hasAccess);
    }

    @GetMapping("/{userId}/status")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    UserStatusResponse status = new UserStatusResponse(
                            user.isActive(),
                            user.getFailedLoginAttempts(),
                            user.isAccountLocked(),
                            user.getLastLogin()
                    );
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record UserStatusResponse(
            boolean active,
            int failedLoginAttempts,
            boolean accountLocked,
            java.time.LocalDateTime lastLogin
    ) {}
}
