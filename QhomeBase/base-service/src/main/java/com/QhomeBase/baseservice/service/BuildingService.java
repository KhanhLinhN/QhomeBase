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

    public List<Building> findAllOrderByCodeAsc() {
        return respo.findAllByOrderByCodeAsc().stream()
                .sorted(Comparator.comparing(Building::getId))
                .collect(Collectors.toList());
    }

    public BuildingDto getBuildingById(UUID buildingId) {
        Building building = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with id: " + buildingId));
        return toDto(building);
    }

    private String generateNextCode() {
        List<Building> Buildings = respo.findAllByOrderByCodeAsc();
        
        // Generate code: B01, B02, B03, etc.
        int nextIndex = Buildings.size() + 1;
        return "B" + String.format("%02d", nextIndex);
    }

    public BuildingDto toDto(Building building) {
        return new BuildingDto(
                building.getId(),
                building.getCode(),
                building.getName(),
                building.getAddress(),
                0,
                0,
                0
        );
    }
    
    public BuildingDto createBuilding(BuildingCreateReq req, String createdBy) {
        String newCode = generateNextCode();

        var b = Building.builder()
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
        
        var existing = respo.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));

        existing.setName(req.name());
        existing.setAddress(req.address());
        existing.setUpdatedBy(u.username());

        Building saved = respo.save(existing);
        return toDto(saved);
    }
}
