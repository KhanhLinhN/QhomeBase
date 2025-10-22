package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import com.QhomeBase.iamservice.service.PermissionService;
import com.QhomeBase.iamservice.service.RoleService;
import com.QhomeBase.iamservice.service.RolePermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserTenantRoleRepository userTenantRoleRepository;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;

    @GetMapping("/all")
    @PreAuthorize("@authz.canViewAllRoles()")
    public ResponseEntity<List<UserRole>> getAllRoles() {

        System.out.println("hi");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions/all")
    @PreAuthorize("@authz.canViewAllPermissions()")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/tenant/{tenantId}/users")
    @PreAuthorize("@authz.canViewTenantRoles(#tenantId)")
    public ResponseEntity<List<UUID>> getUsersInTenant(@PathVariable UUID tenantId) {
        List<UUID> userIds = userTenantRoleRepository.findUserIdsByTenantId(tenantId);
        return ResponseEntity.ok(userIds);
    }

    @GetMapping("/tenant/{tenantId}/role/{role}/users")
    @PreAuthorize("@authz.canViewTenantRoles(#tenantId)")
    public ResponseEntity<List<UUID>> getUsersByRoleInTenant(
            @PathVariable UUID tenantId,
            @PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            List<UUID> userIds = userTenantRoleRepository.findUserIdsByTenantIdAndRole(tenantId, userRole.name().toLowerCase());
            return ResponseEntity.ok(userIds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/tenant/{tenantId}")
    public ResponseEntity<List<String>> getUserRolesInTenant(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        List<String> roles = userTenantRoleRepository.findRolesInTenant(userId, tenantId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/user/{userId}/tenant/{tenantId}/assign")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId,
            @RequestParam String role) {
        return ResponseEntity.status(501).build();
    }

    @DeleteMapping("/user/{userId}/tenant/{tenantId}/remove")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId,
            @RequestParam String role) {
        return ResponseEntity.status(501).build();
    }

    @GetMapping("/permissions/base-service")
    @PreAuthorize("@authz.canViewPermissionsByService('base')")
    public ResponseEntity<List<Permission>> getBaseServicePermissions() {
        return ResponseEntity.ok(permissionService.getBaseServicePermissions());
    }

    @GetMapping("/permissions/iam-service")
    @PreAuthorize("@authz.canViewPermissionsByService('iam')")
    public ResponseEntity<List<Permission>> getIamServicePermissions() {
        return ResponseEntity.ok(permissionService.getIamServicePermissions());
    }

    @GetMapping("/permissions/maintenance-service")
    @PreAuthorize("@authz.canViewPermissionsByService('maintenance')")
    public ResponseEntity<List<Permission>> getMaintenanceServicePermissions() {
        return ResponseEntity.ok(permissionService.getMaintenanceServicePermissions());
    }

    @GetMapping("/permissions/finance-service")
    @PreAuthorize("@authz.canViewPermissionsByService('finance')")
    public ResponseEntity<List<Permission>> getFinanceServicePermissions() {
        return ResponseEntity.ok(permissionService.getFinanceServicePermissions());
    }

    @GetMapping("/permissions/role/{role}")
    @PreAuthorize("@authz.canViewRolePermissions(#role)")
    public ResponseEntity<List<Permission>> getPermissionsByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            List<Permission> permissions = roleService.getPermissionsByRole(userRole);
            return ResponseEntity.ok(permissions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/permissions/account")
    @PreAuthorize("@authz.canViewRolePermissions('account')")
    public ResponseEntity<List<Permission>> getAccountPermissions() {
        return ResponseEntity.ok(roleService.getAccountPermissions());
    }

    @GetMapping("/permissions/admin")
    @PreAuthorize("@authz.canViewRolePermissions('admin')")
    public ResponseEntity<List<Permission>> getAdminPermissions() {
        return ResponseEntity.ok(roleService.getAdminPermissions());
    }

    @GetMapping("/permissions/tenant-owner")
    @PreAuthorize("@authz.canViewRolePermissions('tenant_owner')")
    public ResponseEntity<List<Permission>> getTenantOwnerPermissions() {
        return ResponseEntity.ok(roleService.getTenantOwnerPermissions());
    }

    @GetMapping("/permissions/technician")
    @PreAuthorize("@authz.canViewRolePermissions('technician')")
    public ResponseEntity<List<Permission>> getTechnicianPermissions() {
        return ResponseEntity.ok(roleService.getTechnicianPermissions());
    }

    @GetMapping("/permissions/supporter")
    @PreAuthorize("@authz.canViewRolePermissions('supporter')")
    public ResponseEntity<List<Permission>> getSupporterPermissions() {
        return ResponseEntity.ok(roleService.getSupporterPermissions());
    }

    @PostMapping("/{role}/permissions")
    @PreAuthorize("@authz.canManageRolePermissions(#role)")
    public ResponseEntity<Void> addPermissionToRole(
            @PathVariable String role,
            @RequestBody Map<String, String> request) {
        try {
            String permissionCode = request.get("permissionCode");
            if (permissionCode == null || permissionCode.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            rolePermissionService.addPermissionToRole(role.toLowerCase(), permissionCode);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{role}/permissions/{permissionCode}")
    @PreAuthorize("@authz.canManageRolePermissions(#role)")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable String role,
            @PathVariable String permissionCode) {
        try {
            rolePermissionService.removePermissionFromRole(role.toLowerCase(), permissionCode);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{role}/permissions/batch")
    @PreAuthorize("@authz.canManageRolePermissions(#role)")
    public ResponseEntity<Void> addMultiplePermissionsToRole(
            @PathVariable String role,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> permissionCodes = request.get("permissionCodes");
            if (permissionCodes == null || permissionCodes.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            rolePermissionService.addMultiplePermissionsToRole(role.toLowerCase(), permissionCodes);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{role}/permissions/batch")
    @PreAuthorize("@authz.canManageRolePermissions(#role)")
    public ResponseEntity<Void> removeMultiplePermissionsFromRole(
            @PathVariable String role,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> permissionCodes = request.get("permissionCodes");
            if (permissionCodes == null || permissionCodes.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            rolePermissionService.removeMultiplePermissionsFromRole(role.toLowerCase(), permissionCodes);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{role}/permissions")
    @PreAuthorize("@authz.canManageRolePermissions(#role)")
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable String role,
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> permissionCodes = request.get("permissionCodes");
            if (permissionCodes == null) {
                return ResponseEntity.badRequest().build();
            }
            rolePermissionService.updateRolePermissions(role.toLowerCase(), permissionCodes);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{role}/permissions/check/{permissionCode}")
    @PreAuthorize("@authz.canViewRolePermissions(#role)")
    public ResponseEntity<Map<String, Boolean>> checkRolePermission(
            @PathVariable String role,
            @PathVariable String permissionCode) {
        boolean hasPermission = rolePermissionService.hasPermission(role.toLowerCase(), permissionCode);
        return ResponseEntity.ok(Map.of("hasPermission", hasPermission));
    }
}
