package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGrantService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // Deprecated: Tenant-based permission grants/denies are no longer supported
    @Deprecated
    @Transactional
    public void grantPermissionsToUser(UserPermissionGrantRequest request, Authentication authentication) {
        // No-op: Tenant-based grants are no longer supported
        throw new UnsupportedOperationException("Tenant-based permission grants are no longer supported. Use role-based permissions instead.");
    }

    // Deprecated: Tenant-based permission grants/denies are no longer supported
    @Deprecated
    @Transactional
    public void denyPermissionsFromUser(UserPermissionDenyRequest request, Authentication authentication) {
        // No-op: Tenant-based denies are no longer supported
        throw new UnsupportedOperationException("Tenant-based permission denies are no longer supported. Use role-based permissions instead.");
    }

    // Deprecated: Tenant-based permission grants/denies are no longer supported
    @Deprecated
    @Transactional
    public void revokeGrantsFromUser(UserPermissionRevokeRequest request) {
        // No-op: Tenant-based grants are no longer supported
        throw new UnsupportedOperationException("Tenant-based permission grants are no longer supported.");
    }

    // Deprecated: Tenant-based permission grants/denies are no longer supported
    @Deprecated
    @Transactional
    public void revokeDeniesFromUser(UserPermissionRevokeRequest request) {
        // No-op: Tenant-based denies are no longer supported
        throw new UnsupportedOperationException("Tenant-based permission denies are no longer supported.");
    }

    // Updated to work without tenant: Calculates permissions from User roles directly
    public UserPermissionSummaryDto getUserPermissionSummary(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Get user roles
        List<UserRole> userRoles = user.getRoles();
        List<String> roleNames = userRoles != null && !userRoles.isEmpty()
                ? userRoles.stream()
                    .map(UserRole::getRoleName)
                    .collect(Collectors.toList())
                : List.of();

        // Calculate effective permissions from roles
        Set<String> permissionsSet = new HashSet<>();
        if (userRoles != null && !userRoles.isEmpty()) {
            for (UserRole role : userRoles) {
                List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.getRoleName());
                permissionsSet.addAll(rolePerms);
            }
        }
        List<String> effectivePermissions = new ArrayList<>(permissionsSet);

        // Empty lists for grants/denies (tenant-based grants/denies are no longer supported)
        List<UserPermissionOverrideDto> grants = new ArrayList<>();
        List<UserPermissionOverrideDto> denies = new ArrayList<>();
        List<String> inheritedPermissions = new ArrayList<>(effectivePermissions); // All permissions are inherited from roles now

        return UserPermissionSummaryDto.builder()
                .userId(userId)
                .tenantId(null) // tenantId is no longer used
                .grants(grants)
                .denies(denies)
                .totalGrants(0)
                .totalDenies(0)
                .activeGrants(0)
                .activeDenies(0)
                .temporaryGrants(0)
                .temporaryDenies(0)
                // New fields for frontend
                .inheritedFromRoles(inheritedPermissions)
                .grantedPermissions(List.of())
                .deniedPermissions(List.of())
                // Existing fields
                .effectivePermissions(effectivePermissions)
                .totalEffectivePermissions(effectivePermissions.size())
                .build();
    }

    // Deprecated: Tenant-based grants are no longer supported
    @Deprecated
    public List<String> getActiveGrants(UUID userId, UUID tenantId) {
        return List.of(); // Tenant-based grants are no longer supported
    }

    // Deprecated: Tenant-based denies are no longer supported
    @Deprecated
    public List<String> getActiveDenies(UUID userId, UUID tenantId) {
        return List.of(); // Tenant-based denies are no longer supported
    }

    private Instant convertToInstant(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Instant) {
            return (Instant) obj;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toInstant();
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Instant");
    }
}
