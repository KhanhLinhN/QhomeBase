package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.security.AuthzService;
import com.QhomeBase.iamservice.service.UserTenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/roles")
@RequiredArgsConstructor
public class UserTenantRoleController {
    private final UserTenantRoleService tenantService;
    private final AuthzService authzService;
    
    @GetMapping("/{tenantId}/managers")
    @PreAuthorize("@authz.canViewTenantManagers(#tenantId)")
    public List<UUID> findManagerIdsByTenant(@PathVariable("tenantId") UUID tenantId) {
        return tenantService.getManagers(tenantId);
    }
}
