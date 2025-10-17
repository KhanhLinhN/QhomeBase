package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingDeletionServiceTest {
    
    @Mock 
    private BuildingDeletionRequestRepository buildingDeletionRequestRepository;
    
    @Mock 
    private BuildingRepository buildingRepository;
    
    @Mock 
    private UnitRepository unitRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private BuildingDeletionService buildingDeletionService;
    
    private UUID testTenantId;
    private UUID testBuildingId;
    private UUID testUserId;
    private UUID testRequestId;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testBuildingId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testRequestId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
    }

    @Test
    void doBuildingDeletion_WithValidBuilding_ShouldSuccess() {
        // Arrange
        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.DELETING)
                .build();

        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.ACTIVE)
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of(unit1, unit2));
        when(unitRepository.saveAll(anyList())).thenReturn(List.of(unit1, unit2));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);

        // Act
        buildingDeletionService.doBuildingDeletion(testBuildingId, authentication);

        // Assert
        assertEquals(UnitStatus.INACTIVE, unit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, unit2.getStatus());
        assertEquals(BuildingStatus.ARCHIVED, building.getStatus());

        verify(buildingRepository).findById(testBuildingId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository).saveAll(List.of(unit1, unit2));
        verify(buildingRepository).save(building);
    }

    @Test
    void doBuildingDeletion_WithBuildingNotFound_ShouldThrow() {
        // Arrange
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.doBuildingDeletion(testBuildingId, authentication));
        assertEquals("Building not found", exception.getMessage());

        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(unitRepository);
    }

    @Test
    void doBuildingDeletion_WithBuildingNotDeleting_ShouldThrow() {
        // Arrange
        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.ACTIVE) // Not DELETING
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.doBuildingDeletion(testBuildingId, authentication));
        assertEquals("Building must be in DELETING status to perform deletion tasks", exception.getMessage());

        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(unitRepository);
    }

    @Test
    void doBuildingDeletion_WithNoUnits_ShouldSuccess() {
        // Arrange
        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.DELETING)
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of());
        when(buildingRepository.save(any(Building.class))).thenReturn(building);

        // Act
        buildingDeletionService.doBuildingDeletion(testBuildingId, authentication);

        // Assert
        assertEquals(BuildingStatus.ARCHIVED, building.getStatus());

        verify(buildingRepository).findById(testBuildingId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository, never()).saveAll(anyList());
        verify(buildingRepository).save(building);
    }

    @Test
    void doBuildingDeletion_WithAllUnitsAlreadyInactive_ShouldSuccess() {
        // Arrange
        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.DELETING)
                .build();
        
        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of(unit1, unit2));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);

        // Act
        buildingDeletionService.doBuildingDeletion(testBuildingId, authentication);

        // Assert
        assertEquals(UnitStatus.INACTIVE, unit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, unit2.getStatus());
        assertEquals(BuildingStatus.ARCHIVED, building.getStatus());

        verify(buildingRepository).findById(testBuildingId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository, never()).saveAll(anyList()); // No changes needed
        verify(buildingRepository).save(building);
    }

    @Test
    void completeBuildingDeletion_WithValidRequest_ShouldSuccess() {
        // Arrange
        BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.DELETING)
                .build();

        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(request));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of(unit1, unit2));
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class))).thenReturn(request);

        // Act
        BuildingDeletionRequestDto result = buildingDeletionService.completeBuildingDeletion(testRequestId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(BuildingDeletionStatus.COMPLETED, result.status());
        assertEquals(BuildingStatus.ARCHIVED, building.getStatus());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(buildingRepository).findById(testBuildingId);
        verify(buildingRepository).save(building);
        verify(buildingDeletionRequestRepository).save(request);
    }

    @Test
    void completeBuildingDeletion_WithRequestNotFound_ShouldThrow() {
        // Arrange
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.completeBuildingDeletion(testRequestId, authentication));
        assertEquals("Request not found", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(unitRepository);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void completeBuildingDeletion_WithRequestNotApproved_ShouldThrow() {
        // Arrange
        BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.PENDING) // Not APPROVED
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(request));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.completeBuildingDeletion(testRequestId, authentication));
        assertEquals("Request must be APPROVED before completion", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(unitRepository);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void completeBuildingDeletion_WithUnitsNotInactive_ShouldThrow() {
        // Arrange
        BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.ACTIVE) // Not INACTIVE
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(request));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of(unit1, unit2));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.completeBuildingDeletion(testRequestId, authentication));
        assertTrue(exception.getMessage().contains("All targets must be INACTIVE before completion"));

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void completeBuildingDeletion_WithNoUnits_ShouldSuccess() {
        // Arrange
        BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        Building building = Building.builder()
                .id(testBuildingId)
                .status(BuildingStatus.DELETING)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(request));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(List.of()); // No units
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class))).thenReturn(request);

        // Act
        BuildingDeletionRequestDto result = buildingDeletionService.completeBuildingDeletion(testRequestId, authentication);

        // Assert
        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(BuildingDeletionStatus.COMPLETED, result.status());
        assertEquals(BuildingStatus.ARCHIVED, building.getStatus());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(buildingRepository).findById(testBuildingId);
        verify(buildingRepository).save(building);
        verify(buildingDeletionRequestRepository).save(request);
    }

}