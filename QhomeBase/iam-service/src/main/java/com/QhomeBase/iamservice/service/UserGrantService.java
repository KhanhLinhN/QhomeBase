package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.repository.UserRolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserTenantDenyRepository;
import com.QhomeBase.iamservice.repository.UserTenantGrantRepository;
import com.QhomeBase.iamservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserGrantService {
    private final UserTenantGrantRepository userTenantGrantRepository;
    private final UserTenantDenyRepository userTenantDenyRepository;
    private final UserRolePermissionRepository userRolePermissionRepository;

    @Transactional
    public void grantPermissionsToUser(UserPermissionGrantRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Instant now = Instant.now();
        String grantedBy = principal.username();

        for (String permissionCode : request.getPermissionCodes()) {
            userTenantGrantRepository.upsertGrant(
                    request.getUserId(),
                    request.getTenantId(),
                    permissionCode,
                    request.getExpiresAt(),
                    now,
                    grantedBy,
                    request.getReason()
            );
        }
    }

    @Transactional
    public void denyPermissionsFromUser(UserPermissionDenyRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Instant now = Instant.now();
        String grantedBy = principal.username();

        for (String permissionCode : request.getPermissionCodes()) {
            userTenantDenyRepository.upsertDeny(
                    request.getUserId(),
                    request.getTenantId(),
                    permissionCode,
                    request.getExpiresAt(),
                    now,
                    grantedBy,
                    request.getReason()
            );
        }
    }

    @Transactional
    public void revokeGrantsFromUser(UserPermissionRevokeRequest request) {
        for (String permissionCode : request.getPermissionCodes()) {
            userTenantGrantRepository.removeGrant(
                    request.getUserId(),
                    request.getTenantId(),
                    permissionCode
            );
        }
    }

    @Transactional
    public void revokeDeniesFromUser(UserPermissionRevokeRequest request) {
        for (String permissionCode : request.getPermissionCodes()) {
            userTenantDenyRepository.removeDeny(
                    request.getUserId(),
                    request.getTenantId(),
                    permissionCode
            );
        }
    }

    public UserPermissionSummaryDto getUserPermissionSummary(UUID userId, UUID tenantId) {
        List<Object[]> grantResults = userTenantGrantRepository.findGrantsByUserAndTenant(userId, tenantId);
        List<UserPermissionOverrideDto> grants = new ArrayList<>();
        Instant now = Instant.now();
        
        for (Object[] result : grantResults) {
            String permissionCode = (String) result[0];
            Instant expiresAt = convertToInstant(result[1]);
            Instant grantedAt = convertToInstant(result[2]);
            String grantedBy = (String) result[3];
            String reason = (String) result[4];
            
            boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
            boolean isTemporary = expiresAt != null;
            
            grants.add(UserPermissionOverrideDto.builder()
                    .permissionCode(permissionCode)
                    .granted(true)
                    .grantedAt(grantedAt)
                    .grantedBy(grantedBy)
                    .expiresAt(expiresAt)
                    .isExpired(isExpired)
                    .isTemporary(isTemporary)
                    .reason(reason)
                    .build());
        }

        List<Object[]> denyResults = userTenantDenyRepository.findDeniesByUserAndTenant(userId, tenantId);
        List<UserPermissionOverrideDto> denies = new ArrayList<>();
        
        for (Object[] result : denyResults) {
            String permissionCode = (String) result[0];
            Instant expiresAt = convertToInstant(result[1]);
            Instant grantedAt = convertToInstant(result[2]);
            String grantedBy = (String) result[3];
            String reason = (String) result[4];
            
            boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
            boolean isTemporary = expiresAt != null;
            
            denies.add(UserPermissionOverrideDto.builder()
                    .permissionCode(permissionCode)
                    .granted(false)
                    .grantedAt(grantedAt)
                    .grantedBy(grantedBy)
                    .expiresAt(expiresAt)
                    .isExpired(isExpired)
                    .isTemporary(isTemporary)
                    .reason(reason)
                    .build());
        }

        int activeGrants = userTenantGrantRepository.countActiveGrantsByUserAndTenant(userId, tenantId);
        int activeDenies = userTenantDenyRepository.countActiveDeniesByUserAndTenant(userId, tenantId);
        int temporaryGrants = userTenantGrantRepository.countTemporaryGrantsByUserAndTenant(userId, tenantId);
        
        List<String> effectivePermissions = userRolePermissionRepository
                .getUserRolePermissionsCodeByUserIdAndTenantId(userId, tenantId);

        return UserPermissionSummaryDto.builder()
                .userId(userId)
                .tenantId(tenantId)
                .grants(grants)
                .denies(denies)
                .totalGrants(grants.size())
                .totalDenies(denies.size())
                .activeGrants(activeGrants)
                .activeDenies(activeDenies)
                .temporaryGrants(temporaryGrants)
                .temporaryDenies((int) denies.stream().filter(UserPermissionOverrideDto::isTemporary).count())
                .effectivePermissions(effectivePermissions)
                .totalEffectivePermissions(effectivePermissions.size())
                .build();
    }

    public List<String> getActiveGrants(UUID userId, UUID tenantId) {
        return userTenantGrantRepository.findActiveGrantsByUserAndTenant(userId, tenantId);
    }

    public List<String> getActiveDenies(UUID userId, UUID tenantId) {
        return userTenantDenyRepository.findActiveDeniesByUserAndTenant(userId, tenantId);
    }

    private Instant convertToInstant(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Instant) {
            return (Instant) obj;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toInstant();
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Instant");
    }
}
