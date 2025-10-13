package com.QhomeBase.baseservice.service;


import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.buildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnitService {
    private final UnitRepository unitRepository;
    private final buildingRepository buildingRepository;

    public UnitDto createUnit(UnitCreateDto unitCreateDto) {

        String generatedCode = generateNextCode(unitCreateDto.buildingId(), unitCreateDto.floor());
        var unit = Unit.builder()
                .tenantId(unitCreateDto.tenantId())
                .building(buildingRepository.findById(unitCreateDto.buildingId()).orElseThrow())
                .code(generatedCode)
                .floor(unitCreateDto.floor())
                .areaM2(unitCreateDto.areaM2())
                .bedrooms(unitCreateDto.bedrooms())
                .status(UnitStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();


        var savedUnit = unitRepository.save(unit);


        return toDto(savedUnit);
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
        System.out.println("--test mậdsfnlsf--");

        String expectedPrefix = getPrefix(tenantId, buildingId);
        String expectedStart  = expectedPrefix + floorNumber;

        System.out.println(expectedPrefix+"expectedPrefix");
        System.out.println(expectedStart+ "expectedStart");
        var units = unitRepository.findByBuildingIdAndFloorNumber(buildingId, floorNumber);

        int maxNow = 0;

        for (var unit : units) {
            String code = unit.getCode();
           
            if (code == null) continue;

            if (!code.startsWith(expectedStart)) continue;
            String sequencePart = code.substring(expectedStart.length());
            System.out.println(sequencePart + " sequencePart");
            if (sequencePart.isEmpty()) continue; 



            try {
                int now = Integer.parseInt(sequencePart);
                if (now > maxNow) maxNow = now;
            } catch (NumberFormatException ignore) {

            }
        }


        System.out.println(maxNow);
        return String.format("%02d", maxNow + 1);
    }



    public String generateNextCode(UUID buildingId, int floorNumber) {
        var building = buildingRepository.findById(buildingId).orElseThrow();
        UUID tenantid = building.getTenantId();
        String prefix = getPrefix(tenantid, buildingId);
        String sequence = nextSequence(buildingId, floorNumber);
        return prefix + floorNumber + "---"+ sequence;

    }

    public UnitDto toDto(Unit unit) {
        return new UnitDto(
                unit.getId(),
                unit.getTenantId(),
                unit.getBuilding().getId(),
                unit.getBuilding().getCode(),
                unit.getBuilding().getName(),
                unit.getCode(),
                unit.getFloor(),
                unit.getAreaM2(),
                unit.getBedrooms(),
                unit.getStatus(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }
}
