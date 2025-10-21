package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.service.TenantRolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant-role-permissions")
@RequiredArgsConstructor
public class TenantRolePermissionController {

    private final TenantRolePermissionService tenantRolePermissionService;



    /**
     * Get all effective permissions in a tenant (across all roles)
     * Combines base permissions + tenant grants - tenant denies
     */
    @GetMapping("/permissions/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<List<String>> getAllEffectivePermissionsInTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantRolePermissionService.getAllEffectivePermissionsInTenant(tenantId));
    }

    @PostMapping("/grant/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> grantPermissionsToRole(
            @PathVariable UUID tenantId,
            @RequestBody RolePermissionGrantRequest request, Authentication authentication) {
        tenantRolePermissionService.grantPermissionsToRole(tenantId, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> revokePermissionsFromRole(
            @PathVariable UUID tenantId,
            @RequestBody RolePermissionRevokeRequest request,
            Authentication authentication) {
        tenantRolePermissionService.revokePermissionsFromRole(tenantId, request, authentication);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary/{tenantId}/{role}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<RolePermissionSummaryDto> getRolePermissionSummary(
            @PathVariable UUID tenantId,
            @PathVariable String role) {
        RolePermissionSummaryDto summary = tenantRolePermissionService.getRolePermissionSummary(tenantId, role);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{tenantId}/{role}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<List<RolePermissionDto>> getRolePermissions(
            @PathVariable UUID tenantId,
            @PathVariable String role) {
        List<RolePermissionDto> permissions = tenantRolePermissionService.getRolePermissions(tenantId, role);
        return ResponseEntity.ok(permissions);
    }

    @DeleteMapping("/{tenantId}/{role}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> removeAllPermissionsForRole(
            @PathVariable UUID tenantId,
            @PathVariable String role) {
        tenantRolePermissionService.removeAllPermissionsForRole(tenantId, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles/{tenantId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<List<String>> getRolesWithPermissionsInTenant(@PathVariable UUID tenantId) {
        List<String> roles = tenantRolePermissionService.getRolesWithPermissionsInTenant(tenantId);
        return ResponseEntity.ok(roles);
    }
}
