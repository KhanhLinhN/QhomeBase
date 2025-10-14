package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.buildingRepository;
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
public class buildingService {

    private final buildingRepository respo;

    public buildingService(buildingRepository respo) {
        this.respo = respo;
    }

    public List<building> findAllByTenantIdOrderByCodeAsc(UUID tenantId) {
        return respo.findAllByTenantIdOrderByCodeAsc(tenantId).stream()
                .sorted(Comparator.comparing(building::getId))
                .collect(Collectors.toList());
    }

    private String generateNextCode(UUID tenantId) {
        List<building> buildings = respo.findAllByTenantIdOrderByCodeAsc(tenantId);
        String tenantCode = getTenantCode(tenantId);

        int nextIndex = buildings.size() + 1;
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

    public BuildingDto toDto(building building) {
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
        var u = (UserPrincipal) auth.getPrincipal();
        String newCode = generateNextCode(u.tenant());

        var b = building.builder()
                .tenantId(u.tenant())
                .code(newCode)
                .name(req.name())
                .address(req.address())
                .createdBy(u.username())
                .build();
        building saved = respo.save(b);

        return toDto(saved);
    }
    public BuildingDto updateBuilding(BuildingUpdateReq req, Authentication auth) {
        var u = (UserPrincipal) auth.getPrincipal();
        var b = building.builder()
                .tenantId(u.tenant())
                .name(req.name())
                .address(req.address())
                .createdBy(u.username())
                .build();
        building saved = respo.save(b);

        return toDto(saved);
    }
}
