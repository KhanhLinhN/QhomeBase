package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.AvailablePermissionDto;
import com.QhomeBase.iamservice.dto.AvailableRoleDto;
import com.QhomeBase.iamservice.dto.EmployeeRoleDto;
import com.QhomeBase.iamservice.dto.RoleRemovalRequest;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserTenantRole;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolesRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeRoleManagementService {

    private final UserRepository userRepository;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RolesRepository rolesRepository;


    @Transactional
    public void unassignAllEmployeesFromTenant(UUID tenantId) {
        List<UserTenantRole> tenantRoles = userTenantRoleRepository.findByTenantId(tenantId);

        if (!tenantRoles.isEmpty()) {
            userTenantRoleRepository.deleteAll(tenantRoles);
        }


        userRolePermissionRepository.deleteByTenantId(tenantId);
    }

    public List<EmployeeRoleDto> getEmployeesInTenant(UUID tenantId) {
        List<UUID> userIds = userTenantRoleRepository.findUserIdsByTenantId(tenantId);
        return userRepository.findAllById(userIds)
                .stream()
                .map(user -> mapToEmployeeRoleDto(user, tenantId, null))
                .collect(Collectors.toList());
    }


    public EmployeeRoleDto getEmployeeDetails(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(userId);
        if (!userTenants.contains(tenantId)) {
            throw new IllegalArgumentException("User does not belong to this tenant");
        }
        
        return mapToEmployeeRoleDto(user, tenantId, null);
    }


    public List<AvailableRoleDto> getAvailableRolesForTenant(UUID tenantId) {
        return getGlobalRoles();
    }

    public List<AvailablePermissionDto> getAvailablePermissionsGroupedByService() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(permission -> {
                    String code = permission.getCode();
                    int dotIndex = code.indexOf('.');
                    return dotIndex > 0 ? code.substring(0, dotIndex) : "general";
                }))
                .entrySet().stream()
                .map(entry -> {
                    String servicePrefix = entry.getKey();
                    String serviceName = getServiceDisplayName(servicePrefix);
                    List<String> permissions = entry.getValue().stream()
                            .map(permission -> permission.getCode())
                            .sorted()
                            .collect(Collectors.toList());
                    
                    return AvailablePermissionDto.builder()
                            .servicePrefix(servicePrefix)
                            .serviceName(serviceName)
                            .permissions(permissions)
                            .build();
                })
                .sorted((a, b) -> a.getServicePrefix().compareTo(b.getServicePrefix()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRolesToEmployee(UUID userId, UUID tenantId, List<String> roleNames, String assignedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(user.getId());

        

        for (String roleName : roleNames) {
            if (!rolesRepository.isValidRole(roleName)) {
                throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: admin, tenant_owner, technician, supporter, account");
            }
        }

        for (String roleName : roleNames) {
            assignRoleToUser(user.getId(), tenantId, roleName, assignedBy);
        }
    }

    @Transactional
    public void removeRolesFromEmployee(RoleRemovalRequest request, String removedBy) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<UUID> userTenants = userTenantRoleRepository.findTenantIdsByUserId(user.getId());
        if (!userTenants.contains(request.getTenantId())) {
            throw new IllegalArgumentException("User does not belong to this tenant");
        }
        
        for (String roleName : request.getRoleNames()) {
            removeRoleFromUser(user.getId(), request.getTenantId(), roleName);
        }
    }

    public List<String> getEmployeePermissions(UUID userId, UUID tenantId) {
        return userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(userId, tenantId);
    }

    private EmployeeRoleDto mapToEmployeeRoleDto(User user, UUID tenantId, String assignedBy) {

        List<String> tenantRoles = userTenantRoleRepository.findRolesInTenant(user.getId(), tenantId);
        

        List<EmployeeRoleDto.RoleAssignmentDto> assignedRoles = tenantRoles.stream()
                .filter(role -> role != null && !role.isEmpty())
                .map(roleName -> EmployeeRoleDto.RoleAssignmentDto.builder()
                        .roleName(roleName)
                        .roleDescription("Role description for " + roleName)
                        .assignedAt(Instant.now())
                        .assignedBy(assignedBy != null ? assignedBy : "System")
                        .isActive(true)
                        .build())
                .collect(Collectors.toList());

        List<String> permissions = getEmployeePermissions(user.getId(), tenantId);
        
        return EmployeeRoleDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(null)
                .email(user.getEmail())
                .phoneNumber(null)
                .department(null)
                .position(null)
                .tenantId(tenantId)
                .tenantName("Building " + tenantId)
                .assignedRoles(assignedRoles)
                .allPermissions(permissions)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build();
    }

    private void assignRoleToUser(UUID userId, UUID tenantId, String roleName, String assignedBy) {
        List<String> existingRoles = userTenantRoleRepository.findRolesInTenant(userId, tenantId);
        if (existingRoles.contains(roleName)) {
            throw new IllegalArgumentException("Role " + roleName + " is already assigned to this user");
        }
        
        UserTenantRole userTenantRole = new UserTenantRole();
        userTenantRole.setUserId(userId);
        userTenantRole.setTenantId(tenantId);
        userTenantRole.setRole(roleName);
        userTenantRole.setGrantedAt(Instant.now());
        userTenantRole.setGrantedBy(assignedBy);
        
        userTenantRoleRepository.save(userTenantRole);
    }

    private void removeRoleFromUser(UUID userId, UUID tenantId, String roleName) {
        userTenantRoleRepository.deleteByUserIdAndTenantIdAndRole(userId, tenantId, roleName);
    }

    private String getServiceDisplayName(String servicePrefix) {
        return switch (servicePrefix) {
            case "base" -> "Base Service";
            case "iam" -> "IAM Service";
            case "maintenance" -> "Maintenance Service";
            case "finance" -> "Finance Service";
            case "customer" -> "Customer Service";
            case "document" -> "Document Service";
            case "report" -> "Report Service";
            case "system" -> "System Service";
            default -> servicePrefix.toUpperCase() + " Service";
        };
    }

    private List<AvailableRoleDto> getGlobalRoles() {
        List<Object[]> roleData = rolesRepository.findGlobalRoles();
        
        return roleData.stream()
                .map(row -> {
                    String roleName = (String) row[0];
                    String description = (String) row[1];
                    

                    List<String> permissions = rolesRepository.findPermissionsByRole(roleName);
                    

                    String category = determineRoleCategory(roleName);
                    
                    return AvailableRoleDto.builder()
                            .roleName(roleName)
                            .description(description != null ? description : getDefaultDescription(roleName))
                            .permissions(permissions)
                            .isAssignable(true)
                            .category(category)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private String determineRoleCategory(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "admin" -> "ADMIN";
            case "tenant_owner" -> "MANAGER";
            case "technician", "supporter", "account" -> "STAFF";
            default -> "STAFF";
        };
    }
    
    private String getDefaultDescription(String roleName) {
        return switch (roleName.toLowerCase()) {
            case "admin" -> "System Administrator";
            case "tenant_owner" -> "Tenant Owner";
            case "technician" -> "Technician";
            case "supporter" -> "Supporter";
            case "account" -> "Account Manager";
            default -> roleName + " Role";
        };
    }
}
