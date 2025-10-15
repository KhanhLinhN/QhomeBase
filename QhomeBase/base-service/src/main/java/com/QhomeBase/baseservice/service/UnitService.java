package com.QhomeBase.baseservice.service;


import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnitService {
    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    
    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UnitDto createUnit(UnitCreateDto unitCreateDto) {
        validateUnitCreateDto(unitCreateDto);
        
        String generatedCode = generateNextCode(unitCreateDto.buildingId(), unitCreateDto.floor());
        var unit = Unit.builder()
                .tenantId(unitCreateDto.tenantId())
                .building(buildingRepository.findById(unitCreateDto.buildingId()).orElseThrow())
                .code(generatedCode)
                .floor(unitCreateDto.floor())
                .areaM2(unitCreateDto.areaM2())
                .bedrooms(unitCreateDto.bedrooms())
                .status(UnitStatus.ACTIVE)
                .createdAt(nowUTC())
                .updatedAt(nowUTC())
                .build();
        var savedUnit = unitRepository.save(unit);
        return toDto(savedUnit);
    }
    public UnitDto updateUnit(UnitUpdateDto unit, UUID id) {
        validateUnitUpdateDto(unit);
        
        Unit existingUnit = unitRepository.findByIdWithBuilding(id);

        if (unit.floor() != null) {
            existingUnit.setFloor(unit.floor());
        }
        if (unit.areaM2() != null) {
            existingUnit.setAreaM2(unit.areaM2());
        }
        if (unit.bedrooms() != null) {
            existingUnit.setBedrooms(unit.bedrooms());
        }
        
        existingUnit.setUpdatedAt(nowUTC());
        
        var savedUnit = unitRepository.save(existingUnit);
        return toDto(savedUnit);
    }
    
    public void deleteUnit(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow();
        unit.setStatus(UnitStatus.INACTIVE);
        unit.setUpdatedAt(nowUTC());
        
        unitRepository.save(unit);
    }
    

    
    public UnitDto getUnitById(UUID id) {
        Unit unit = unitRepository.findByIdWithBuilding(id);
        return toDto(unit);
    }
    
    public java.util.List<UnitDto> getUnitsByBuildingId(UUID buildingId) {
        var units = unitRepository.findAllByBuildingId(buildingId);
        return units.stream()
                .map(this::toDto)
                .toList();
    }
    
    public java.util.List<UnitDto> getUnitsByTenantId(UUID tenantId) {
        var units = unitRepository.findAllByTenantId(tenantId);
        return units.stream()
                .map(this::toDto)
                .toList();
    }
    
    public java.util.List<UnitDto> getUnitsByFloor(UUID buildingId, Integer floor) {
        var units = unitRepository.findByBuildingIdAndFloorNumber(buildingId, floor);
        return units.stream()
                .map(this::toDto)
                .toList();
    }
    
    public void changeUnitStatus(UUID id, UnitStatus newStatus) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow();
        
        unit.setStatus(newStatus);
        unit.setUpdatedAt(nowUTC());
        
        unitRepository.save(unit);
    }
    
    public String getPrefix(UUID tenantId, UUID buildingId) {
        var buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantId);
        int index = 0;
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i).getId().equals(buildingId)) {
                index = i;
            }
        }
        return String.valueOf((char) ('A' + index));
    }

    public String nextSequence(UUID buildingId, int floorNumber) {
        UUID tenantId = buildingRepository.findTenantIdByBuilding(buildingId);
        String expectedPrefix = getPrefix(tenantId, buildingId);
        String expectedStart  = expectedPrefix + floorNumber;

        var units = unitRepository.findByBuildingIdAndFloorNumber(buildingId, floorNumber);

        int maxNow = 0;

        for (var unit : units) {
            String code = unit.getCode();
            if (code == null) continue;

            if (!code.startsWith(expectedStart)) continue;
            String sequencePart = code.substring(expectedStart.length());
            if (sequencePart.isEmpty()) continue;
            String cleanSequence = cleanSequenceString(sequencePart);
            if (cleanSequence.isEmpty()) continue;
            
            try {
                int now = Integer.parseInt(cleanSequence);
                if (now > maxNow) maxNow = now;
            } catch (Exception e) {
            }
        }
        return String.format("%02d", maxNow + 1);
    }

    private String cleanSequenceString(String sequencePart) {
        if (sequencePart == null || sequencePart.isEmpty()) {
            return "";
        }

        StringBuilder cleanSequence = new StringBuilder();
        for (int i = sequencePart.length() - 1; i >= 0; i--) {
            char c = sequencePart.charAt(i);
            if (Character.isDigit(c)) {
                cleanSequence.insert(0, c);
            } else {
                break;
            }
        }
        
        return cleanSequence.toString();
    }

    public String generateNextCode(UUID buildingId, int floorNumber) {
        var building = buildingRepository.findById(buildingId).orElseThrow();
        UUID tenantid = building.getTenantId();
        String prefix = getPrefix(tenantid, buildingId);
        String sequence = nextSequence(buildingId, floorNumber);
        return prefix + floorNumber + "---"+ sequence;
    }

    public UnitDto toDto(Unit unit) {
        String buildingId = null;
        String buildingCode = null;
        String buildingName = null;
        try {
            if (unit.getBuilding() != null) {
                buildingId = unit.getBuilding().getId().toString();
                buildingCode = unit.getBuilding().getCode();
                buildingName = unit.getBuilding().getName();
            }
        } catch (Exception e) {
        }
        
        return new UnitDto(
                unit.getId(),
                unit.getTenantId(),
                buildingId != null ? UUID.fromString(buildingId) : null,
                buildingCode,
                buildingName,
                unit.getCode(),
                unit.getFloor(),
                unit.getAreaM2(),
                unit.getBedrooms(),
                unit.getStatus(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }

    private void validateUnitCreateDto(UnitCreateDto dto) {
        if (dto.tenantId() == null) {
            throw new NullPointerException("Tenant ID cannot be null");
        }
        if (dto.buildingId() == null) {
            throw new NullPointerException("Building ID cannot be null");
        }
        if (dto.floor() == null) {
            throw new NullPointerException("Floor cannot be null");
        }
        if (dto.floor() <= 0) {
            throw new IllegalArgumentException("Floor must be positive");
        }
        if (dto.floor() != Math.floor(dto.floor())) {
            throw new IllegalArgumentException("Floor must be an integer");
        }
        if (dto.areaM2() == null) {
            throw new NullPointerException("Area cannot be null");
        }
        if (dto.areaM2().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }
        if (dto.bedrooms() == null) {
            throw new NullPointerException("Bedrooms cannot be null");
        }
        if (dto.bedrooms() <= 0) {
            throw new IllegalArgumentException("Bedrooms must be positive");
        }
        if (dto.bedrooms() != Math.floor(dto.bedrooms())) {
            throw new IllegalArgumentException("Bedrooms must be an integer");
        }
    }

    private void validateUnitUpdateDto(UnitUpdateDto dto) {
        if (dto.floor() == null) {
            throw new NullPointerException("Floor cannot be null");
        }
        if (dto.floor() <= 0) {
            throw new IllegalArgumentException("Floor must be positive");
        }
        if (dto.floor() != Math.floor(dto.floor())) {
            throw new IllegalArgumentException("Floor must be an integer");
        }
        if (dto.areaM2() == null) {
            throw new NullPointerException("Area cannot be null");
        }
        if (dto.areaM2().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Area must be positive");
        }
        if (dto.bedrooms() == null) {
            throw new NullPointerException("Bedrooms cannot be null");
        }
        if (dto.bedrooms() <= 0) {
            throw new IllegalArgumentException("Bedrooms must be positive");
        }
        if (dto.bedrooms() != Math.floor(dto.bedrooms())) {
            throw new IllegalArgumentException("Bedrooms must be an integer");
        }
    }
}
