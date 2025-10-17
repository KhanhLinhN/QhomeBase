package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.TenantRepository;
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
    private final TenantRepository tenantRepository;

    public BuildingService(BuildingRepository respo, TenantRepository tenantRepository) {
        this.respo = respo;
        this.tenantRepository = tenantRepository;
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
    public BuildingDto createBuilding(BuildingCreateReq req, UUID tenantId, String createdBy) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant with ID " + tenantId + " does not exist");
        }
        
        String newCode = generateNextCode(tenantId);

        var b = Building.builder()
                .tenantId(tenantId)
                .code(newCode)
                .name(req.name())
                .address(req.address())
                .createdBy(createdBy)
                .build();
        Building saved = respo.save(b);

        return toDto(saved);
    }
    public BuildingDto updateBuilding(UUID buildingId, BuildingUpdateReq req, Authentication auth) {
        var u = (UserPrincipal) auth.getPrincipal();
        

        if (!tenantRepository.existsById(u.tenant())) {
            throw new IllegalArgumentException("Tenant with ID " + u.tenant() + " does not exist");
        }
        
        var existing = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));

        if (!existing.getTenantId().equals(u.tenant())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        existing.setName(req.name());
        existing.setAddress(req.address());
        existing.setUpdatedBy(u.username());

        Building saved = respo.save(existing);
        return toDto(saved);
    }
}
