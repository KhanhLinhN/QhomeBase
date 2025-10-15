package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildingService {

    private final BuildingRepository respo;

    public BuildingService(BuildingRepository respo) {
        this.respo = respo;
    }

    public List<Building> findAllByTenantIdOrderByCodeAsc(UUID tenantId) {
        return respo.findAllByTenantIdOrderByCodeAsc(tenantId).stream()
                .sorted(Comparator.comparing(Building::getId))
                .collect(Collectors.toList());
    }

    private String generateNextCode(UUID tenantId) {
        List<Building> Buildings = respo.findAllByTenantIdOrderByCodeAsc(tenantId);
        String tenantCode = getTenantCode(tenantId);

        int nextIndex = Buildings.size() + 1;
        return tenantCode + String.format("%02d", nextIndex);
    }

    private String getTenantCode(UUID tenantId) {
        try {
            String tenantCode = String.valueOf(respo.findTenantCodeByTenantId(tenantId));
            if (tenantCode != null && !tenantCode.trim().isEmpty()) {
                return tenantCode.trim();
            }
        } catch (Exception e) {

        }
        return "Tenant";
    }

    public BuildingDto toDto(Building building) {
        return new BuildingDto(
                building.getId(),
                building.getTenantId(),
                building.getCode(),
                building.getName(),
                building.getAddress(),
                0,
                0,
                0
        );
    }
    public BuildingDto createBuilding(BuildingCreateReq req, Authentication auth) {
        validateBuildingName(req.name());
        validateBuildingAddress(req.address());
        
        var u = (UserPrincipal) auth.getPrincipal();
        String newCode = generateNextCode(u.tenant());

        var b = Building.builder()
                .tenantId(u.tenant())
                .code(newCode)
                .name(req.name())
                .address(req.address())
                .createdBy(u.username())
                .build();
        Building saved = respo.save(b);

        return toDto(saved);
    }
    public BuildingDto updateBuilding(BuildingUpdateReq req, Authentication auth) {
        validateBuildingName(req.name());
        validateBuildingAddress(req.address());
        
        var u = (UserPrincipal) auth.getPrincipal();
        var b = Building.builder()
                .tenantId(u.tenant())
                .name(req.name())
                .address(req.address())
                .createdBy(u.username())
                .build();
        Building saved = respo.save(b);

        return toDto(saved);
    }

    private void validateBuildingName(String name) {
        if (name == null) {
            throw new NullPointerException("Building name cannot be null");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Building name cannot be empty");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Building name cannot exceed 255 characters");
        }
    }

    private void validateBuildingAddress(String address) {
        if (address != null && address.length() > 512) {
            throw new IllegalArgumentException("Building address cannot exceed 512 characters");
        }
    }
}
