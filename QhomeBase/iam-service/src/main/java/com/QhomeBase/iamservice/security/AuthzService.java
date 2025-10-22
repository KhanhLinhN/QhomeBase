package com.QhomeBase.iamservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service("authz")
public class AuthzService {
    
    private UserPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        
        var principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        
        return null;
    }
    
    private boolean hasPerm(String perm) {
        var p = principal();
        return p != null && p.perms() != null && p.perms().contains(perm);
    }

    private boolean hasAnyRole(Set<String> rolesNeed) {
        var p = principal();
        if (p == null || p.roles() == null) return false;
        for (String r : rolesNeed) {
            if (p.roles().contains(r)) return true;
        }
        return false;
    }

    private boolean sameTenant(UUID tenantId) {
        var p = principal();
        return p != null && p.tenant() != null && p.tenant().equals(tenantId);
    }

    
    public boolean canCreateUser() {
        return hasPerm("iam.user.create");
    }
    
    public boolean canViewUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.read") || hasAnyRole(Set.of("admin")) || (p != null && p.uid().equals(userId));
    }
    
    public boolean canViewAllUsers() {
        return hasPerm("iam.user.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canUpdateUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.update") || hasAnyRole(Set.of("admin")) || (p != null && p.uid().equals(userId));
    }
    
    public boolean canDeleteUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.delete") || hasAnyRole(Set.of("admin")) || (p != null && p.uid().equals(userId));
    }
    
    public boolean canManageUserRoles(UUID userId) {
        return hasPerm("iam.user.role.manage") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canViewUserPermissions(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.permission.read") || hasAnyRole(Set.of("admin")) || (p != null && p.uid().equals(userId));
    }
    
    public boolean canResetUserPassword(UUID userId) {
        return hasPerm("iam.user.password.reset") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canLockUserAccount(UUID userId) {
        return hasPerm("iam.user.account.lock") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canUnlockUserAccount(UUID userId) {
        return hasPerm("iam.user.account.unlock") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }

    
    public boolean canViewAllRoles() {
        return hasPerm("iam.role.read") || hasAnyRole(Set.of("admin", "tenant_owner", "technician", "supporter"));
    }
    
    public boolean canCreateRole() {
        return hasPerm("iam.role.create") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canUpdateRole(String roleName) {
        return hasPerm("iam.role.update") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canDeleteRole(String roleName) {
        return hasPerm("iam.role.delete") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canAssignRoleToUser(UUID userId) {
        return hasPerm("iam.role.assign") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canRemoveRoleFromUser(UUID userId) {
        return hasPerm("iam.role.remove") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canViewRolePermissions(String roleName) {
        return hasPerm("iam.role.permission.read") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canManageRolePermissions(String roleName) {
        return hasPerm("iam.role.permission.manage") || hasAnyRole(Set.of("admin"));
    }

    
    public boolean canViewAllPermissions() {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canCreatePermission() {
        return hasPerm("iam.permission.create") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canUpdatePermission(String permissionCode) {
        return hasPerm("iam.permission.update") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canDeletePermission(String permissionCode) {
        return hasPerm("iam.permission.delete") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canViewPermissionsByService(String servicePrefix) {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canViewPermissionByCode(String permissionCode) {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }

    
    public boolean canViewTenantRoles(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canAssignTenantRole(UUID tenantId, UUID userId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.assign") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canRemoveTenantRole(UUID tenantId, UUID userId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.remove") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canCreateTenantRole(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.create") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canUpdateTenantRole(UUID tenantId, String roleName) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.update") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canDeleteTenantRole(UUID tenantId, String roleName) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.role.delete") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canViewTenantManagers(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.tenant.manager.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }

    
    public boolean canLogin() {
        return true;
    }
    
    public boolean canRefreshToken() {
        return true;
    }
    
    public boolean canLogout() {
        return true;
    }
    
    public boolean canChangePassword(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.password.change") || hasAnyRole(Set.of("admin")) || (p != null && p.uid().equals(userId));
    }
    
    public boolean canViewOwnProfile() {
        return true;
    }
    
    public boolean canUpdateOwnProfile() {
        return true;
    }

    
    public boolean canViewSystemStats() {
        return hasPerm("iam.system.stats.read") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canManageSystemSettings() {
        return hasPerm("iam.system.settings.manage") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canViewAuditLogs() {
        return hasPerm("iam.system.audit.read") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canExportData() {
        return hasPerm("iam.system.data.export") || hasAnyRole(Set.of("admin", "tenant_owner"));
    }
    
    public boolean canImportData() {
        return hasPerm("iam.system.data.import") || hasAnyRole(Set.of("admin"));
    }

    
    public boolean canGenerateTestToken() {
        return hasPerm("iam.test.generate_token") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canAccessTestEndpoints() {
        return hasPerm("iam.test.access") || hasAnyRole(Set.of("admin"));
    }
    
    public boolean canViewUserInfo() {
        return true;
    }

    
    public boolean isAdmin() {
        return hasAnyRole(Set.of("admin"));
    }
    
    public boolean isTenantOwner() {
        return hasAnyRole(Set.of("tenant_owner"));
    }
    
    public boolean isTechnician() {
        return hasAnyRole(Set.of("technician"));
    }
    
    public boolean isSupporter() {
        return hasAnyRole(Set.of("supporter"));
    }
    
    public boolean isAccount() {
        return hasAnyRole(Set.of("account"));
    }
    
    public UUID getCurrentUserId() {
        return principal().uid();
    }
    
    public UUID getCurrentTenantId() {
        return principal().tenant();
    }
    

    public boolean canViewTenantEmployees(UUID tenantId) {
        if (hasAnyRole(Set.of("admin"))) {
            return true;
        }
        
        boolean sameTenant = sameTenant(tenantId);
        boolean isTenantOwner = hasAnyRole(Set.of("tenant_owner"));
        return sameTenant && isTenantOwner;
    }
    
    public boolean canAssignEmployeeRole(UUID tenantId, UUID employeeId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.role.assign") || hasAnyRole(Set.of("admin", "tenant_owner"));
        
        if (hasAnyRole(Set.of("admin"))) {
            return true;
        }
        
        return sameTenant && okRole;
    }
    
    public boolean canRemoveEmployeeRole(UUID tenantId, UUID employeeId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.role.remove") || hasAnyRole(Set.of("admin", "tenant_owner"));
        
        if (hasAnyRole(Set.of("admin"))) {
            return true;
        }
        
        return sameTenant && okRole;
    }
    
    public boolean canViewEmployeeDetails(UUID tenantId, UUID employeeId) {
        var p = principal();
        
        if (hasAnyRole(Set.of("admin"))) {
            return true;
        }
        
        if (p != null && p.uid().equals(employeeId)) {
            return true;
        }
        
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.read") || hasAnyRole(Set.of("tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageDepartment(UUID tenantId, String department) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManagePermissions(UUID tenantId) {
        if (hasAnyRole(Set.of("admin"))) {
            return true;
        }
        
        boolean sameTenant = sameTenant(tenantId);
        boolean isTenantOwner = hasAnyRole(Set.of("tenant_owner"));
        return sameTenant && isTenantOwner;
    }
    
    public boolean canBulkAssignRoles(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.role.bulk_assign") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canExportEmployeeList(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.export") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canImportEmployeeList(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.import") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageTenantEmployees(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("iam.employee.manage") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageBuilding(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("base.building.manage") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageUnits(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("base.unit.manage") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageResidents(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("base.resident.manage") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean canManageFees(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("finance.fee.manage") || hasAnyRole(Set.of("admin", "tenant_owner", "account"));
        return sameTenant && okRole;
    }
    
    public boolean canApproveResidents(UUID tenantId) {
        boolean sameTenant = sameTenant(tenantId);
        boolean okRole = hasPerm("base.resident.approve") || hasAnyRole(Set.of("admin", "tenant_owner"));
        return sameTenant && okRole;
    }
    
    public boolean isBuildingManager() {
        return hasAnyRole(Set.of("tenant_owner"));
    }
}
