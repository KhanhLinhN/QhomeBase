package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
public class UserTenantRoleService {
    private UserTenantRoleRepository userTenantRoleRepository;

    public List<UUID> getManagers(UUID tenantId) {
        return userTenantRoleRepository.findManagerIdsByTenant(tenantId);
    }
}
