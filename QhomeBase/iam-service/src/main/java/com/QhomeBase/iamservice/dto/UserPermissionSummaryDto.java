package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionSummaryDto {
    private UUID userId;
    private UUID tenantId;
    
    // User-level overrides
    private List<UserPermissionOverrideDto> grants;
    private List<UserPermissionOverrideDto> denies;
    
    // Counts
    private int totalGrants;
    private int totalDenies;
    private int activeGrants;
    private int activeDenies;
    private int temporaryGrants;
    private int temporaryDenies;
    
    // Final effective permissions (after all calculations)
    private List<String> effectivePermissions;
    private int totalEffectivePermissions;
}

