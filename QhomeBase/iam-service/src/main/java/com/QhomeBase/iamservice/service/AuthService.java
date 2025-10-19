package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.repository.UserTenantRoleRepository;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.security.JwtIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByUsername(loginRequestDto.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(loginRequestDto.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (user.isAccountLocked()) {
            throw new IllegalArgumentException("User account is locked due to multiple failed login attempts");
        }

        List<UUID> tenantIds = userTenantRoleRepository.findTenantIdsByUserId(user.getId());
        if (tenantIds.isEmpty()) {
            throw new IllegalArgumentException("User has no access to any tenant");
        }

        UUID selectedTenantId = validateTenantAccess(loginRequestDto.tennantId(), tenantIds);
        List<String> userRoles = userTenantRoleRepository.findRolesInTenant(user.getId(), selectedTenantId);

        if (userRoles.isEmpty()) {
            throw new IllegalArgumentException("User has no roles in the selected tenant");
        }

        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        List<String> userPermissions = userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(user.getId(), selectedTenantId);
        
        String accessToken = jwtIssuer.issueForService(
                user.getId(),
                user.getUsername(),
                selectedTenantId,
                userRoles,
                userPermissions,
                "base-service,finance-service,customer-service,iam-service"
        );

        return new LoginResponseDto(
                accessToken,
                "Bearer",
                3600L,
                java.time.Instant.now().plusSeconds(3600),
                new UserInfoDto(
                        user.getId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        selectedTenantId != null ? selectedTenantId.toString() : null,
                        null,
                        userRoles,
                        userPermissions
                )
        );
    }

    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();
        userRepository.save(user);
    }

    private UUID validateTenantAccess(UUID requestedTenantId, List<UUID> userTenantIds) {
        if (requestedTenantId != null) {
            if (userTenantIds.contains(requestedTenantId)) {
                return requestedTenantId;
            } else {
                throw new IllegalArgumentException("User does not have access to the requested tenant");
            }
        } else {
            if (userTenantIds.size() == 1) {
                return userTenantIds.get(0);
            } else {
                throw new IllegalArgumentException("Multiple tenants available, please specify tenant ID");
            }
        }
    }

    public void logout(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void refreshToken(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        List<UUID> tenantIds = userTenantRoleRepository.findTenantIdsByUserId(user.getId());
        if (!tenantIds.contains(tenantId)) {
            throw new IllegalArgumentException("User does not have access to the specified tenant");
        }

        List<String> userRoles = userTenantRoleRepository.findRolesInTenant(user.getId(), tenantId);
        if (userRoles.isEmpty()) {
            throw new IllegalArgumentException("User has no roles in the specified tenant");
        }

        List<String> userPermissions = userRolePermissionRepository.getUserRolePermissionsCodeByUserIdAndTenantId(user.getId(), tenantId);
        
        jwtIssuer.issueForService(
                user.getId(),
                user.getUsername(),
                tenantId,
                userRoles,
                userPermissions,
                "base-service,finance-service,customer-service,iam-service"
        );
    }

    public List<UUID> getUserTenants(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return userTenantRoleRepository.findTenantIdsByUserId(userId);
    }

    public List<String> getUserRolesInTenant(UUID userId, UUID tenantId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return userTenantRoleRepository.findRolesInTenant(userId, tenantId);
    }

    public boolean validateUserAccess(UUID userId, UUID tenantId) {
        List<UUID> userTenantIds = userTenantRoleRepository.findTenantIdsByUserId(userId);
        return userTenantIds.contains(tenantId);
    }
}
