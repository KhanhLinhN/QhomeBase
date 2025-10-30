package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnitServiceTest {

    @Mock 
    private UnitRepository unitRepository;
    
    @Mock 
    private BuildingRepository buildingRepository;
    
    @Mock 
    private TenantRepository tenantRepository;

    private UnitService unitService;
    
    private UUID testTenantId;
    private UUID testBuildingId;
    private UUID testUnitId;

    @BeforeEach
    void setUp() {
        unitService = new UnitService(unitRepository, buildingRepository, tenantRepository);

        testTenantId = UUID.randomUUID();
        testBuildingId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();
    }

    @Test
    void createUnit_WithValidData_ShouldReturnUnitDto() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("50.5"),
                2
        );
        
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("A")
                .name("Building A")
                .build();

        Unit savedUnit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .building(building)
                .code("A101")
                .floor(1)
                .areaM2(new BigDecimal("50.5"))
                .bedrooms(2)
                .status(UnitStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(buildingRepository.findAllByTenantIdOrderByCodeAsc(testTenantId))
                .thenReturn(List.of(building));
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingRepository.findTenantIdByBuilding(testBuildingId)).thenReturn(testTenantId);
        when(unitRepository.findByBuildingIdAndFloorNumber(testBuildingId, 1))
                .thenReturn(List.of());
        when(unitRepository.save(any(Unit.class))).thenReturn(savedUnit);

        UnitDto result = unitService.createUnit(request);

        assertNotNull(result);
        assertEquals(testUnitId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals("A", result.buildingCode());
        assertEquals("Building A", result.buildingName());
        assertEquals("A101", result.code());
        assertEquals(1, result.floor());
        assertEquals(new BigDecimal("50.5"), result.areaM2());
        assertEquals(2, result.bedrooms());
        assertEquals(UnitStatus.ACTIVE, result.status());

        verify(buildingRepository, times(2)).findAllByTenantIdOrderByCodeAsc(testTenantId);
        verify(buildingRepository, times(2)).findById(testBuildingId);
        verify(buildingRepository).findTenantIdByBuilding(testBuildingId);
        verify(unitRepository).findByBuildingIdAndFloorNumber(testBuildingId, 1);
        verify(unitRepository).save(any(Unit.class));
    }

    @Test
    void createUnit_WithNullTenantId_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                null,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("50.5"),
                2
        );

        assertThrows(NullPointerException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNullBuildingId_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                null,
                "A101",
                1,
                new BigDecimal("50.5"),
                2
        );

        assertThrows(NullPointerException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNullFloor_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                null,
                new BigDecimal("50.5"),
                2
        );

        assertThrows(NullPointerException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNullArea_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                null,
                2
        );

        assertThrows(NullPointerException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNullBedrooms_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("50.5"),
                null
        );

        assertThrows(NullPointerException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithZeroFloor_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                0,
                new BigDecimal("50.5"),
                2
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNegativeFloor_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                -1,
                new BigDecimal("50.5"),
                2
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNegativeArea_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("-50.5"),
                2
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithZeroBedrooms_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("50.5"),
                0
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithNegativeBedrooms_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("50.5"),
                -1
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }

    @Test
    void createUnit_WithZeroArea_ShouldThrow() {
        UnitCreateDto request = new UnitCreateDto(
                testTenantId,
                testBuildingId,
                "A101",
                1,
                new BigDecimal("0"),
                2
        );

        assertThrows(IllegalArgumentException.class,
                () -> unitService.createUnit(request));

        verifyNoInteractions(unitRepository, buildingRepository);
    }


    @Test
    void updateUnit_WithValidData_ShouldReturnUnitDto() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("60.0"), 3);
        
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("A")
                .name("Building A")
                .build();

        Unit existingUnit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .building(building)
                .code("A101")
                .floor(1)
                .areaM2(new BigDecimal("50.5"))
                .bedrooms(2)
                .status(UnitStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Unit savedUnit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .building(building)
                .code("A101")
                .floor(2)
                .areaM2(new BigDecimal("60.0"))
                .bedrooms(3)
                .status(UnitStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(unitRepository.findByIdWithBuilding(testUnitId)).thenReturn(existingUnit);
        when(unitRepository.save(any(Unit.class))).thenReturn(savedUnit);

        UnitDto result = unitService.updateUnit(request, testUnitId);

        assertNotNull(result);
        assertEquals(testUnitId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(2, result.floor());
        assertEquals(new BigDecimal("60.0"), result.areaM2());
        assertEquals(3, result.bedrooms());

        verify(unitRepository).findByIdWithBuilding(testUnitId);
        verify(unitRepository).save(any(Unit.class));
    }

    @Test
    void updateUnit_WithNullFloor_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(null, new BigDecimal("60.0"), 3);

        assertThrows(NullPointerException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithNullArea_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, null, 3);

        assertThrows(NullPointerException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithNullBedrooms_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("60.0"), null);

        assertThrows(NullPointerException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithZeroFloor_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(0, new BigDecimal("60.0"), 3);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithNegativeFloor_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(-1, new BigDecimal("60.0"), 3);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithNegativeArea_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("-60.0"), 3);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithZeroBedrooms_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("60.0"), 0);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithNegativeBedrooms_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("60.0"), -1);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateUnit_WithZeroArea_ShouldThrow() {
        UnitUpdateDto request = new UnitUpdateDto(2, new BigDecimal("0"), 3);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.updateUnit(request, testUnitId));

        verifyNoInteractions(unitRepository);
    }


    @Test
    void deleteUnit_WithValidId_ShouldSetStatusToInactive() {
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("A")
                .name("Building A")
                .build();

        Unit existingUnit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .building(building)
                .code("A101")
                .floor(1)
                .areaM2(new BigDecimal("50.5"))
                .bedrooms(2)
                .status(UnitStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(existingUnit));
        when(unitRepository.save(any(Unit.class))).thenReturn(existingUnit);

        unitService.deleteUnit(testUnitId);

        assertEquals(UnitStatus.INACTIVE, existingUnit.getStatus());
        assertNotNull(existingUnit.getUpdatedAt());

        verify(unitRepository).findById(testUnitId);
        verify(unitRepository).save(existingUnit);
    }


    @Test
    void getUnitsByTenantId_WithValidTenantId_ShouldReturnList() {
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("A")
                .name("Building A")
                .build();

        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .building(building)
                .code("A101")
                .floor(1)
                .status(UnitStatus.ACTIVE)
                .build();

        when(unitRepository.findAllByTenantId(testTenantId))
                .thenReturn(List.of(unit1));

        List<UnitDto> result = unitService.getUnitsByTenantId(testTenantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(unit1.getId(), result.get(0).id());
        assertEquals(testTenantId, result.get(0).tenantId());

        verify(unitRepository).findAllByTenantId(testTenantId);
    }

}