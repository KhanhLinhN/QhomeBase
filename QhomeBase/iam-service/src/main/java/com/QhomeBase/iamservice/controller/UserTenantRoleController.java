package com.QhomeBase.iamservice.controller;


import com.QhomeBase.iamservice.service.UserTenantRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/roles")

public class UserTenantRoleController {
    private UserTenantRoleService tenantService;
    @GetMapping("/{tenantId}/managers")
    public List<UUID> findManagerIdsByTenant(@PathVariable("tenantId") UUID tenantId) {
        return tenantService.getManagers(tenantId);
    }
}
