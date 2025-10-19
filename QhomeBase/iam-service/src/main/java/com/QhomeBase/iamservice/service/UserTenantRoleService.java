package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTenantRoleService {
    private final UserTenantRoleRepository userTenantRoleRepository;

    public List<UUID> getManagers(UUID tenantId) {
        return userTenantRoleRepository.findTenantIdsByUserId(tenantId);
    }
}
