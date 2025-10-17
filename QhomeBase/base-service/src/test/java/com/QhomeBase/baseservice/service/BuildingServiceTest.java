/*package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuildingServiceTest {

    @Mock 
    private BuildingRepository buildingRepository;
    
    @Mock 
    private Authentication authentication;
    
    @Mock 
    private UserPrincipal userPrincipal;

    private BuildingService buildingService;
    
    private UUID testTenantId;
    private UUID testBuildingId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testBuildingId = UUID.randomUUID();
        testUsername = "testuser";

        buildingService = new BuildingService(buildingRepository);

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.tenant()).thenReturn(testTenantId);
        when(userPrincipal.username()).thenReturn(testUsername);
    }

    @Test
    void createBuilding_WithValidData_ShouldReturnBuildingDto() {
        String buildingName = "FPT Tower";
        String buildingAddress = "123 Hoa Lac, Thach That, Ha Noi";
        String tenantCode = "FPT";
        String expectedCode = "FPT01";
        
        BuildingCreateReq request = new BuildingCreateReq(buildingName, buildingAddress);
        
        Building savedBuilding = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code(expectedCode)
                .name(buildingName)
                .address(buildingAddress)
                .status(BuildingStatus.ACTIVE)
                .createdBy(testUsername)
                .isDeleted(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(buildingRepository.findAllByTenantIdOrderByCodeAsc(testTenantId))
                .thenReturn(List.of());
        when(buildingRepository.findTenantCodeByTenantId(testTenantId))
                .thenReturn(Optional.of(tenantCode));
        when(buildingRepository.save(any(Building.class)))
                .thenReturn(savedBuilding);

        BuildingDto result = buildingService.createBuilding(request, authentication);

        assertNotNull(result);
        assertEquals(testBuildingId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(expectedCode, result.code());
        assertEquals(buildingName, result.name());
        assertEquals(buildingAddress, result.address());
        assertEquals(0, result.floorsMax());
        assertEquals(0, result.totalApartmentsAll());
        assertEquals(0, result.totalApartmentsActive());

        verify(buildingRepository).findAllByTenantIdOrderByCodeAsc(testTenantId);
        verify(buildingRepository).findTenantCodeByTenantId(testTenantId);
        verify(buildingRepository).save(any(Building.class));
    }



    @Test
    void updateBuilding_WithValidData_ShouldReturnBuildingDto() {
        String buildingName = "Updated FPT Tower";
        String buildingAddress = "456 Hoa Lac, Thach That, Ha Noi";
        
        BuildingUpdateReq request = new BuildingUpdateReq(buildingName, buildingAddress);
        
        Building savedBuilding = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("FPT01")
                .name(buildingName)
                .address(buildingAddress)
                .status(BuildingStatus.ACTIVE)
                .createdBy(testUsername)
                .isDeleted(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(buildingRepository.findById(testBuildingId))
                .thenReturn(java.util.Optional.of(
                        Building.builder()
                                .id(testBuildingId)
                                .tenantId(testTenantId)
                                .code("FPT01")
                                .name("Old Name")
                                .address("Old Address")
                                .createdBy(testUsername)
                                .build()
                ));
        when(buildingRepository.save(any(Building.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BuildingDto result = buildingService.updateBuilding(testBuildingId, request, authentication);

        assertNotNull(result);
        assertEquals(testBuildingId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(buildingName, result.name());
        assertEquals(buildingAddress, result.address());

        verify(buildingRepository).findById(testBuildingId);
        verify(buildingRepository).save(any(Building.class));
    }

    @Test
    void createBuilding_WithNullName_ShouldThrow() {
        BuildingCreateReq request = new BuildingCreateReq(null, "Valid address");

        assertThrows(NullPointerException.class,
                () -> buildingService.createBuilding(request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void createBuilding_WithEmptyName_ShouldThrow() {
        BuildingCreateReq request = new BuildingCreateReq("", "Valid address");

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.createBuilding(request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void createBuilding_WithNameTooLong_ShouldThrow() {
        String longName = "A".repeat(256);
        BuildingCreateReq request = new BuildingCreateReq(longName, "Valid address");

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.createBuilding(request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void createBuilding_WithAddressTooLong_ShouldThrow() {
        String longAddress = "A".repeat(513);
        BuildingCreateReq request = new BuildingCreateReq("Valid name", longAddress);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.createBuilding(request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void updateBuilding_WithNullName_ShouldThrow() {
        BuildingUpdateReq request = new BuildingUpdateReq(null, "Valid address");

        assertThrows(NullPointerException.class,
                () -> buildingService.updateBuilding(testBuildingId, request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void updateBuilding_WithEmptyName_ShouldThrow() {
        BuildingUpdateReq request = new BuildingUpdateReq("", "Valid address");

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.updateBuilding(testBuildingId, request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void updateBuilding_WithNameTooLong_ShouldThrow() {
        String longName = "A".repeat(256);
        BuildingUpdateReq request = new BuildingUpdateReq(longName, "Valid address");

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.updateBuilding(testBuildingId, request, authentication));

        verifyNoInteractions(buildingRepository);
    }

    @Test
    void updateBuilding_WithAddressTooLong_ShouldThrow() {
        String longAddress = "A".repeat(513);
        BuildingUpdateReq request = new BuildingUpdateReq("Valid name", longAddress);

        assertThrows(IllegalArgumentException.class,
                () -> buildingService.updateBuilding(testBuildingId, request, authentication));

        verifyNoInteractions(buildingRepository);
    }



}
*/
