package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.RolePermissionDto;
import com.QhomeBase.iamservice.dto.RolePermissionGrantRequest;
import com.QhomeBase.iamservice.dto.RolePermissionRevokeRequest;
import com.QhomeBase.iamservice.dto.RolePermissionSummaryDto;
import com.QhomeBase.iamservice.repository.TenantRolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantRolePermissionService {

    private final TenantRolePermissionRepository tenantRolePermissionRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;


    public List<String> getSelectedRolesInTenant(UUID tenantId) {
        return tenantRolePermissionRepository.getSelectedRoleInTenant(tenantId);
    }

    @Transactional
    public void grantPermissionsToRole(UUID tenantId, RolePermissionGrantRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Instant now = Instant.now();
        String grantedBy = principal.username();

        for (String permissionCode : request.getPermissionCodes()) {
            tenantRolePermissionRepository.upsertPermission(
                    tenantId, request.getRole(), permissionCode, true, now, grantedBy
            );
        }
    }

    @Transactional
    public void revokePermissionsFromRole(UUID tenantId, RolePermissionRevokeRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        for (String permissionCode : request.getPermissionCodes()) {
            tenantRolePermissionRepository.removePermission(tenantId, request.getRole(), permissionCode);
        }
    }

    public List<String> getAllEffectivePermissionsInTenant(UUID tenantId) {
        return tenantRolePermissionRepository.findAllEffectivePermissionsInTenant(tenantId);
    }

    public RolePermissionSummaryDto getRolePermissionSummary(UUID tenantId, String role) {
        List<String> grantedPermissions = tenantRolePermissionRepository.findGrantedPermissionsByTenantAndRole(tenantId, role);
        List<String> deniedPermissions = tenantRolePermissionRepository.findDeniedPermissionsByTenantAndRole(tenantId, role);

        List<String> effectivePermissions = getEffectivePermissionsForRole(tenantId, role);

        return RolePermissionSummaryDto.builder()
                .role(role)
                .tenantId(tenantId)
                .grantedPermissions(grantedPermissions)
                .deniedPermissions(deniedPermissions)
                .effectivePermissions(effectivePermissions)
                .totalPermissions(effectivePermissions.size())
                .build();
    }

    public List<RolePermissionDto> getRolePermissions(UUID tenantId, String role) {
        List<Object[]> results = tenantRolePermissionRepository.findPermissionsByTenantAndRole(tenantId, role);

        return results.stream()
                .map(result -> RolePermissionDto.builder()
                        .role(role)
                        .permissionCode((String) result[0])
                        .granted((Boolean) result[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeAllPermissionsForRole(UUID tenantId, String role) {
        tenantRolePermissionRepository.removeAllPermissionsForRole(tenantId, role);
    }

    public List<String> getRolesWithPermissionsInTenant(UUID tenantId) {
        return tenantRolePermissionRepository.findRolesWithPermissionsInTenant(tenantId);
    }

    private List<String> getEffectivePermissionsForRole(UUID tenantId, String role) {
        return userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), tenantId
        );
    }
}



