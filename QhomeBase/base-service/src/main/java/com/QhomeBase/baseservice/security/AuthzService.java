package com.QhomeBase.baseservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service("authz")
public class AuthzService {
    private UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    private boolean hasPerm(String perm) {
        var p = principal();
        return p.perms() != null && p.perms().contains(perm);
    }

    private boolean hasAnyRole(Set<String> rolesNeed) {
        var p = principal();
        if (p.roles() == null) return false;
        for (String r : rolesNeed) {
            if (p.roles().contains(r)) return true;
        }
        return false;
    }

    private boolean sameTenant(UUID tenantId) {
        var p = principal();
        return p.tenant() != null && p.tenant().equals(tenantId);
    }

    public boolean canManageTenant(UUID tenantId) {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean sameTenant = p.tenant().equals(tenantId);
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.request"));
        return sameTenant && (okRole || okPerm);
    }

    public boolean canApproveTicket(UUID ticketTenantId) {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean sameTenant = p.tenant().equals(ticketTenantId);
        boolean okRole = p.roles() != null && (p.roles().contains("tenant_manager") || p.roles().contains("tenant_owner"));
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.tenant.delete.approve"));
        return sameTenant && (okRole || okPerm);
    }
    public boolean canCreateBuilding() {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.building.create"));
        
        return okPerm;
    }
    public boolean canUpdateBuilding() {
        var p = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean okPerm = p.perms() != null && p.perms().stream().anyMatch(s -> s.equals("base.building.update"));



        return okPerm;
    }
    public boolean canRequestDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.request");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canApproveDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canDeleteBuilding() {
        boolean okPerm = hasPerm("base.building.delete");
        return okPerm;
    }

    public boolean canRejectDeleteBuilding(UUID tenantId) {
        boolean st = sameTenant(tenantId);
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        boolean result = st && (okRole || okPerm);
        return result;
    }

    public boolean canViewDeleteBuilding(UUID id) {

        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okPerm;
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("tenant_manager", "tenant_owner", "admin"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm;
    }

}
