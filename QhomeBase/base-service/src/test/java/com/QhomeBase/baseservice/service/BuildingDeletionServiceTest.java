package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.buildingRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingDeletionServiceTest {

    @Mock
    private BuildingDeletionRequestRepository buildingDeletionRequestRepository;

    @Mock
    private buildingRepository buildingRepository;

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
    }

    @Test
    void testCreate_WithValidData_ShouldCreateDeletionRequest() {
        // Given
        BuildingDeletionCreateReq createReq = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        building testBuilding = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.ACTIVE)
                .build();

        BuildingDeletionRequest savedRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(testBuilding));
        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(testTenantId, testBuildingId))
                .thenReturn(Arrays.asList());
        when(buildingRepository.save(any(building.class))).thenReturn(testBuilding);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class))).thenReturn(savedRequest);

        // When
        BuildingDeletionRequestDto result = buildingDeletionService.create(createReq, authentication);

        // Then
        assertNotNull(result);
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals("Test deletion reason", result.reason());
        assertEquals(BuildingDeletionStatus.PENDING, result.status());
        assertEquals(BuildingStatus.PENDING_DELETION, testBuilding.getStatus());

        verify(buildingRepository).findById(testBuildingId);
        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(testTenantId, testBuildingId);
        verify(buildingRepository).save(testBuilding);
        verify(buildingDeletionRequestRepository).save(any(BuildingDeletionRequest.class));
    }

    @Test
    void testCreate_WithNonExistentBuilding_ShouldThrowException() {
        // Given
        BuildingDeletionCreateReq createReq = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.create(createReq, authentication));

        assertEquals("Building not found", exception.getMessage());
        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(buildingDeletionRequestRepository);
    }

    @Test
    void testCreate_WithBuildingNotBelongingToTenant_ShouldThrowException() {
        // Given
        BuildingDeletionCreateReq createReq = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        UUID differentTenantId = UUID.randomUUID();
        building testBuilding = building.builder()
                .id(testBuildingId)
                .tenantId(differentTenantId)
                .status(BuildingStatus.ACTIVE)
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(testBuilding));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.create(createReq, authentication));

        assertEquals("Building does not belong to the specified tenant", exception.getMessage());
        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(buildingDeletionRequestRepository);
    }

    @Test
    void testCreate_WithExistingPendingRequest_ShouldThrowException() {
        // Given
        BuildingDeletionCreateReq createReq = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        building testBuilding = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.ACTIVE)
                .build();

        BuildingDeletionRequest existingRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.PENDING)
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(testBuilding));
        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(testTenantId, testBuildingId))
                .thenReturn(Arrays.asList(existingRequest));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.create(createReq, authentication));

        assertEquals("There is already a pending deletion request for this building", exception.getMessage());
        verify(buildingRepository).findById(testBuildingId);
        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(testTenantId, testBuildingId);
        verifyNoMoreInteractions(buildingRepository);
        verifyNoMoreInteractions(buildingDeletionRequestRepository);
    }

    @Test
    void testApprove_WithValidData_ShouldApproveRequest() {
        // Given
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("Approved by admin");

        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        building testBuilding = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.PENDING_DELETION)
                .build();

        building testBuildingForUnit = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .build();

        Unit testUnit = Unit.builder()
                .id(UUID.randomUUID())
                .building(testBuildingForUnit)
                .status(UnitStatus.ACTIVE)
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(deletionRequest));
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(testBuilding));
        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(Arrays.asList(testUnit));
        when(unitRepository.saveAll(any())).thenReturn(Arrays.asList(testUnit));
        when(buildingRepository.save(any(building.class))).thenReturn(testBuilding);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class))).thenReturn(deletionRequest);

        // When
        BuildingDeletionRequestDto result = buildingDeletionService.approve(testRequestId, approveReq, authentication);

        // Then
        assertNotNull(result);
        assertEquals(BuildingDeletionStatus.APPROVED, deletionRequest.getStatus());
        assertEquals(BuildingStatus.DELETING, testBuilding.getStatus());
        assertEquals(UnitStatus.INACTIVE, testUnit.getStatus());
        assertEquals("Approved by admin", deletionRequest.getNote());
        assertEquals(testUserId, deletionRequest.getApprovedBy());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(buildingRepository).findById(testBuildingId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository).saveAll(any());
        verify(buildingRepository).save(testBuilding);
        verify(buildingDeletionRequestRepository).save(deletionRequest);
    }

    @Test
    void testApprove_WithNonExistentRequest_ShouldThrowException() {
        // Given
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("Approved by admin");

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));

        assertEquals("Request not found", exception.getMessage());
        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository);
        verifyNoInteractions(unitRepository);
    }

    @Test
    void testApprove_WithNonPendingStatus_ShouldThrowException() {
        // Given
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("Approved by admin");

        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(deletionRequest));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));

        assertEquals("Request is not PENDING", exception.getMessage());
        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository);
        verifyNoInteractions(unitRepository);
    }

    @Test
    void testReject_WithValidData_ShouldRejectRequest() {
        // Given
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq("Rejected by admin");

        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        building testBuilding = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.PENDING_DELETION)
                .build();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(deletionRequest));
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(testBuilding));
        when(buildingRepository.save(any(building.class))).thenReturn(testBuilding);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class))).thenReturn(deletionRequest);

        // When
        BuildingDeletionRequestDto result = buildingDeletionService.reject(testRequestId, rejectReq, authentication);

        // Then
        assertNotNull(result);
        assertEquals(BuildingDeletionStatus.REJECTED, deletionRequest.getStatus());
        assertEquals(BuildingStatus.ACTIVE, testBuilding.getStatus());
        assertEquals("Rejected by admin", deletionRequest.getNote());
        assertEquals(testUserId, deletionRequest.getApprovedBy());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(buildingRepository).findById(testBuildingId);
        verify(buildingRepository).save(testBuilding);
        verify(buildingDeletionRequestRepository).save(deletionRequest);
    }

    @Test
    void testReject_WithNonExistentRequest_ShouldThrowException() {
        // Given
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq("Rejected by admin");

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));

        assertEquals("Request not found", exception.getMessage());
        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void testChangeStatusOfUnitsBuilding_ShouldUpdateActiveUnitsToInactive() {
        // Given
        building testBuildingForUnits = building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .build();

        Unit activeUnit1 = Unit.builder()
                .id(UUID.randomUUID())
                .building(testBuildingForUnits)
                .status(UnitStatus.ACTIVE)
                .build();

        Unit activeUnit2 = Unit.builder()
                .id(UUID.randomUUID())
                .building(testBuildingForUnits)
                .status(UnitStatus.ACTIVE)
                .build();

        Unit inactiveUnit = Unit.builder()
                .id(UUID.randomUUID())
                .building(testBuildingForUnits)
                .status(UnitStatus.INACTIVE)
                .build();

        List<Unit> units = Arrays.asList(activeUnit1, activeUnit2, inactiveUnit);

        when(unitRepository.findAllByBuildingId(testBuildingId)).thenReturn(units);
        when(unitRepository.saveAll(any())).thenReturn(units);

        // When
        buildingDeletionService.changeStatusOfUnitsBuilding(testBuildingId);

        // Then
        assertEquals(UnitStatus.INACTIVE, activeUnit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, activeUnit2.getStatus());
        assertEquals(UnitStatus.INACTIVE, inactiveUnit.getStatus()); // Không thay đổi vì đã INACTIVE

        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository).saveAll(units);
    }

    @Test
    void testGetById_WithValidId_ShouldReturnRequest() {
        // Given
        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.of(deletionRequest));

        // When
        BuildingDeletionRequestDto result = buildingDeletionService.getById(testRequestId);

        // Then
        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals("Test deletion reason", result.reason());
        assertEquals(BuildingDeletionStatus.PENDING, result.status());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
    }

    @Test
    void testGetById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(buildingDeletionRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.getById(testRequestId));

        assertEquals("Request not found", exception.getMessage());
        verify(buildingDeletionRequestRepository).findById(testRequestId);
    }

    @Test
    void testGetByTenantId_ShouldReturnRequestsForTenant() {
        // Given
        BuildingDeletionRequest request1 = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(UUID.randomUUID())
                .status(BuildingDeletionStatus.PENDING)
                .build();

        BuildingDeletionRequest request2 = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(UUID.randomUUID())
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(testTenantId, null))
                .thenReturn(Arrays.asList(request1, request2));

        // When
        List<BuildingDeletionRequestDto> result = buildingDeletionService.getByTenantId(testTenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testTenantId, result.get(0).tenantId());
        assertEquals(testTenantId, result.get(1).tenantId());

        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(testTenantId, null);
    }

    @Test
    void testGetByBuildingId_ShouldReturnRequestsForBuilding() {
        // Given
        BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.PENDING)
                .build();

        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(null, testBuildingId))
                .thenReturn(Arrays.asList(request));

        // When
        List<BuildingDeletionRequestDto> result = buildingDeletionService.getByBuildingId(testBuildingId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testBuildingId, result.get(0).buildingId());

        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(null, testBuildingId);
    }

    @Test
    void testGetPendingRequests_ShouldReturnOnlyPendingRequests() {
        // Given
        BuildingDeletionRequest pendingRequest1 = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.PENDING)
                .build();

        BuildingDeletionRequest pendingRequest2 = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(UUID.randomUUID())
                .status(BuildingDeletionStatus.PENDING)
                .build();

        BuildingDeletionRequest approvedRequest = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(UUID.randomUUID())
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        when(buildingDeletionRequestRepository.findAll())
                .thenReturn(Arrays.asList(pendingRequest1, pendingRequest2, approvedRequest));

        // When
        List<BuildingDeletionRequestDto> result = buildingDeletionService.getPendingRequests();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(req -> req.status() == BuildingDeletionStatus.PENDING));

        verify(buildingDeletionRequestRepository).findAll();
    }
}
