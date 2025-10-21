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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;
    private final UserTenantRoleRepository userTenantRoleRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByUsername(loginRequestDto.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + loginRequestDto.username()));
        log.debug("Found user id={} active={} locked={} failedAttempts={} for username={}",
                user.getId(), user.isActive(), user.isAccountLocked(), user.getFailedLoginAttempts(), loginRequestDto.username());
    // Temporarily allow plain-text password comparison in addition to encoded match
        boolean passwordMatches = passwordEncoder.matches(loginRequestDto.password(), user.getPasswordHash())
                || loginRequestDto.password().equals(user.getPasswordHash());
        if (!passwordMatches) {
            handleFailedLogin(user);
            log.warn("Password mismatch for user={} (failedAttempts={})", user.getUsername(), user.getFailedLoginAttempts());
            throw new IllegalArgumentException("Password mismatch for user: " + loginRequestDto.username());
        }

        if (!user.isActive()) {
            log.warn("User account disabled: {}", user.getUsername());
            throw new IllegalArgumentException("User account is disabled: " + user.getUsername());
        }

        if (user.isAccountLocked()) {
            log.warn("User account locked: {}", user.getUsername());
            throw new IllegalArgumentException("User account is locked: " + user.getUsername());
        }

        List<UUID> tenantIds = userTenantRoleRepository.findTenantIdsByUserId(user.getId());
        if (tenantIds.isEmpty()) {
            log.warn("User has no tenant access: {}", user.getUsername());
            throw new IllegalArgumentException("User has no access to any tenant: " + user.getUsername());
        }

        UUID selectedTenantId = validateTenantAccess(loginRequestDto.tenantId(), tenantIds);
        List<String> userRoles = userTenantRoleRepository.findRolesInTenant(user.getId(), selectedTenantId);
        log.debug("User roles in tenant {}: {}", selectedTenantId, userRoles);

        if (userRoles.isEmpty()) {
            log.warn("User has no roles in tenant {}: {}", selectedTenantId, user.getUsername());
            throw new IllegalArgumentException("User has no roles in the selected tenant: " + selectedTenantId);
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
                throw new IllegalArgumentException("No access to requested tenant: " + requestedTenantId);
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
