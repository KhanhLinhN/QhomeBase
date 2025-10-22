package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.EmployeeDto;
import com.QhomeBase.iamservice.dto.EmployeePermissionStatus;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import com.QhomeBase.iamservice.repository.UserTenantGrantRepository;
import com.QhomeBase.iamservice.repository.UserTenantDenyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeManagementService {

    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final UserTenantGrantRepository userTenantGrantRepository;
    private final UserTenantDenyRepository userTenantDenyRepository;

    public List<EmployeeDto> getEmployeesInTenant(UUID tenantId) {
        List<UUID> userIds = userTenantRoleRepository.findUserIdsByTenantId(tenantId);
        return userRepository.findAllById(userIds)
                .stream()
                .map(user -> mapToEmployeeDto(user, tenantId))
                .collect(Collectors.toList());
    }

    public EmployeeDto getEmployeeDetails(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(userId);
        if (!userTenants.contains(tenantId)) {
            throw new IllegalArgumentException("User does not belong to this tenant");
        }
        
        return mapToEmployeeDto(user, tenantId);
    }

    @Transactional
    public EmployeeDto assignEmployeeToTenant(UUID userId, UUID tenantId, String assignedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(userId);
        if (userTenants.contains(tenantId)) {
            throw new IllegalArgumentException("User is already assigned to this tenant");
        }
        
        userTenantRoleRepository.assignUserToTenant(userId, tenantId, java.time.Instant.now(), assignedBy != null ? assignedBy : "System");
        return mapToEmployeeDto(user, tenantId);
    }

    @Transactional
    public void removeEmployeeFromTenant(UUID userId, UUID tenantId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(userId);
        if (!userTenants.contains(tenantId)) {
            throw new IllegalArgumentException("User does not belong to this tenant");
        }
        
        userTenantRoleRepository.removeUserFromTenant(userId, tenantId);
    }

    public List<EmployeeDto> getAvailableEmployees() {
        List<UUID> assignedUserIds = userTenantRoleRepository.findAllAssignedUserIds();
        return userRepository.findAll()
                .stream()
                .filter(user -> !assignedUserIds.contains(user.getId()))
                .map(user -> mapToEmployeeDto(user, null))
                .collect(Collectors.toList());
    }

    public long countEmployeesInTenant(UUID tenantId) {
        return userTenantRoleRepository.countUsersInTenant(tenantId);
    }

    private EmployeeDto mapToEmployeeDto(User user, UUID tenantId) {
        List<String> globalRoles = userTenantRoleRepository.findGlobalRolesByUserId(user.getId());
        String roleInfo = globalRoles.isEmpty() ? "No roles" : String.join(", ", globalRoles);
        
        EmployeePermissionStatus permissionStatus = EmployeePermissionStatus.STANDARD;
        int grantedOverrides = 0;
        int deniedOverrides = 0;
        boolean hasTemporaryPermissions = false;
        Instant lastPermissionChange = null;
        
        if (tenantId != null) {
            grantedOverrides = userTenantGrantRepository.countActiveGrantsByUserAndTenant(user.getId(), tenantId);
            deniedOverrides = userTenantDenyRepository.countActiveDeniesByUserAndTenant(user.getId(), tenantId);
            hasTemporaryPermissions = userTenantGrantRepository.countTemporaryGrantsByUserAndTenant(user.getId(), tenantId) > 0;
            
            permissionStatus = EmployeePermissionStatus.determineStatus(grantedOverrides, deniedOverrides, hasTemporaryPermissions);
        }
        
        return EmployeeDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(null)
                .email(user.getEmail())
                .phoneNumber(null)
                .department(null)
                .position(roleInfo)
                .tenantId(tenantId)
                .tenantName(tenantId != null ? "Building " + tenantId : null)
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .permissionStatus(permissionStatus)
                .grantedOverrides(grantedOverrides)
                .deniedOverrides(deniedOverrides)
                .totalOverrides(grantedOverrides + deniedOverrides)
                .hasTemporaryPermissions(hasTemporaryPermissions)
                .lastPermissionChange(lastPermissionChange)
                .build();
    }
}
