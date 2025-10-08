package com.QhomeBase.baseservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("authz")
public class AuthzService {

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
}
