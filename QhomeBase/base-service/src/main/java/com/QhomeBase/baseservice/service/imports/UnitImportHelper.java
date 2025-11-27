package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.imports.UnitImportRowResult;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnitImportHelper {
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final UnitService unitService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UnitImportRowResult importSingleUnit(String buildingCode, Integer floor, BigDecimal areaM2, Integer bedrooms, int excelRow) {
        Building building = resolveBuilding(buildingCode, excelRow);
        UUID buildingId = building.getId();
        validateUnitData(floor, areaM2, bedrooms, building, excelRow);
        var dto = unitService.createUnit(new UnitCreateDto(buildingId, null, floor, areaM2, bedrooms));
        Unit created = unitRepository.findById(dto.id()).orElseThrow();
        Unit createdWithBuilding = unitRepository.findByIdWithBuilding(created.getId());

        return UnitImportRowResult.builder()
                .rowNumber(excelRow)
                .success(true)
                .message("OK")
                .unitId(createdWithBuilding.getId().toString())
                .buildingId(createdWithBuilding.getBuilding().getId().toString())
                .buildingCode(createdWithBuilding.getBuilding().getCode())
                .code(createdWithBuilding.getCode())
                .build();
    }

    private Building resolveBuilding(String buildingCode, int rowNumber) {
        if (buildingCode == null || buildingCode.isBlank()) {
            throw new IllegalArgumentException("BuildingCode (row " + rowNumber + ") không được để trống");
        }
        Optional<Building> found = buildingRepository.findAllByOrderByCodeAsc()
                .stream()
                .filter(b -> buildingCode.trim().equalsIgnoreCase(b.getCode()))
                .findFirst();
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy Building với code: " + buildingCode.trim() + " (row " + rowNumber + ")");
        }
        return found.get();
    }

    private void validateUnitData(Integer floor, BigDecimal areaM2, Integer bedrooms, Building building, int rowNumber) {
        validateFloor(floor, building, rowNumber);
        validateAreaM2(areaM2, rowNumber);
        validateBedrooms(bedrooms, rowNumber);
    }

    private void validateFloor(Integer floor, Building building, int rowNumber) {
        if (floor == null) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") không được để trống");
        }
        if (floor < 0) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") phải lớn hơn hoặc bằng 0");
        }
        if (building.getNumberOfFloors() != null && floor > building.getNumberOfFloors()) {
            throw new IllegalArgumentException(
                    String.format("Floor %d (row %d) vượt quá số tầng của tòa nhà %s (%d tầng)", 
                            floor, rowNumber, building.getCode(), building.getNumberOfFloors()));
        }
        if (floor > 200) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") không được vượt quá 200");
        }
    }

    private void validateAreaM2(java.math.BigDecimal areaM2, int rowNumber) {
        if (areaM2 == null) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") không được để trống");
        }
        if (areaM2.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") phải lớn hơn 0");
        }
        if (areaM2.compareTo(new java.math.BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") không được vượt quá 10000 m²");
        }
    }

    private void validateBedrooms(Integer bedrooms, int rowNumber) {
        if (bedrooms == null) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") không được để trống");
        }
        if (bedrooms < 0) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") phải lớn hơn hoặc bằng 0");
        }
        if (bedrooms > 20) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") không được vượt quá 20");
        }
    }
}

