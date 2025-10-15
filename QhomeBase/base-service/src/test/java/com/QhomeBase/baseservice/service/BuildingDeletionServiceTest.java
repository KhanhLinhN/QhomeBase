package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
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

    private BuildingDeletionService buildingDeletionService;
    
    private UUID testTenantId;
    private UUID testBuildingId;
    private UUID testUserId;
    private UUID testRequestId;

    @BeforeEach
    void setUp() {
        buildingDeletionService = new BuildingDeletionService(
                buildingDeletionRequestRepository, 
                buildingRepository, 
                unitRepository
        );

        testTenantId = UUID.randomUUID();
        testBuildingId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testRequestId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
    }

    @Test
    void create_WithValidData_ShouldSuccess() {
        String reason = "Building no longer needed";
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(testTenantId, testBuildingId, reason);
        
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .code("FPT01")
                .name("FPT Tower")
                .status(BuildingStatus.ACTIVE)
                .build();


        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(testTenantId, testBuildingId))
                .thenReturn(List.of());
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class)))
                .thenAnswer(invocation -> {
                    BuildingDeletionRequest req = invocation.getArgument(0);
                    req.setId(testRequestId);
                    req.setCreatedAt(OffsetDateTime.now());
                    return req;
                });

        BuildingDeletionRequestDto result = buildingDeletionService.create(request, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals(reason, result.reason());
        assertEquals(BuildingDeletionStatus.PENDING, result.status());
        assertNotNull(result.createdAt());

        verify(buildingRepository).findById(testBuildingId);
        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(testTenantId, testBuildingId);
        verify(buildingRepository).save(any(Building.class));
        verify(buildingDeletionRequestRepository).save(any(BuildingDeletionRequest.class));
    }

    @Test
    void create_WithBuildingNotFound_ShouldThrow() {
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(testTenantId, testBuildingId, "reason");
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.create(request, authentication));
        assertEquals("Building not found", exception.getMessage());

        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(buildingDeletionRequestRepository);
    }

    @Test
    void create_WithBuildingNotBelongToTenant_ShouldThrow() {
        UUID differentTenantId = UUID.randomUUID();
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(differentTenantId, testBuildingId, "reason");
        
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId) // Different tenant
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.create(request, authentication));
        assertEquals("Building does not belong to the specified tenant", exception.getMessage());

        verify(buildingRepository).findById(testBuildingId);
        verifyNoInteractions(buildingDeletionRequestRepository);
    }

    @Test
    void create_WithExistingPendingRequest_ShouldThrow() {
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(testTenantId, testBuildingId, "reason");
        
        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .build();

        BuildingDeletionRequest existingRequest = BuildingDeletionRequest.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .status(BuildingDeletionStatus.PENDING)
                .build();

        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingDeletionRequestRepository.findByTenantIdAndBuildingId(testTenantId, testBuildingId))
                .thenReturn(List.of(existingRequest));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.create(request, authentication));
        assertEquals("There is already a pending deletion request for this building", exception.getMessage());

        verify(buildingRepository).findById(testBuildingId);
        verify(buildingDeletionRequestRepository).findByTenantIdAndBuildingId(testTenantId, testBuildingId);
        verify(buildingRepository, never()).save(any());
    }


    @Test
    void approve_WithValidPendingRequest_ShouldReturnApprovedDTO() {
        String note = "Approved for deletion";
        UUID approverId = UUID.randomUUID();
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq(note);
        
        BuildingDeletionRequest pendingRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.PENDING_DELETION)
                .build();
        
        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .building(building)
                .status(UnitStatus.ACTIVE)
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .building(building)
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.of(pendingRequest));
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(approverId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(unitRepository.findAllByBuildingId(testBuildingId))
                .thenReturn(List.of(unit1, unit2));
        when(unitRepository.saveAll(anyList())).thenReturn(List.of(unit1, unit2));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class)))
                .thenReturn(pendingRequest);

        BuildingDeletionRequestDto result = buildingDeletionService.approve(testRequestId, approveReq, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals(approverId, result.approvedBy());
        assertEquals(note, result.note());
        assertEquals(BuildingDeletionStatus.APPROVED, result.status());
        assertNotNull(result.approvedAt());

        assertEquals(BuildingStatus.DELETING, building.getStatus());
        assertEquals(UnitStatus.INACTIVE, unit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, unit2.getStatus());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(buildingRepository).findById(testBuildingId);
        verify(unitRepository).findAllByBuildingId(testBuildingId);
        verify(unitRepository).saveAll(anyList());
        verify(buildingRepository).save(any(Building.class));
        verify(buildingDeletionRequestRepository).save(any(BuildingDeletionRequest.class));
    }

    @Test
    void approve_WithRequestNotFound_ShouldThrow() {
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("note");
        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));
        assertEquals("Request not found", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository, unitRepository);
    }

    @Test
    void approve_WithNonPendingRequest_ShouldThrow() {
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("note");
        BuildingDeletionRequest approvedRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .status(BuildingDeletionStatus.APPROVED)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.of(approvedRequest));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));
        assertEquals("Request is not PENDING", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository, unitRepository);
    }


    @Test
    void reject_WithValidPendingRequest_ShouldReturnRejectedDTO() {
        String note = "Rejected for specific reasons";
        UUID approverId = UUID.randomUUID();
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq(note);
        
        BuildingDeletionRequest pendingRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test reason")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        Building building = Building.builder()
                .id(testBuildingId)
                .tenantId(testTenantId)
                .status(BuildingStatus.PENDING_DELETION)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.of(pendingRequest));
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(approverId);
        when(buildingRepository.findById(testBuildingId)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingDeletionRequestRepository.save(any(BuildingDeletionRequest.class)))
                .thenReturn(pendingRequest);

        BuildingDeletionRequestDto result = buildingDeletionService.reject(testRequestId, rejectReq, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testBuildingId, result.buildingId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals(approverId, result.approvedBy());
        assertEquals(note, result.note());
        assertEquals(BuildingDeletionStatus.REJECTED, result.status());
        assertNotNull(result.approvedAt());

        assertEquals(BuildingStatus.ACTIVE, building.getStatus());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verify(buildingRepository).findById(testBuildingId);
        verify(buildingRepository).save(any(Building.class));
        verify(buildingDeletionRequestRepository).save(any(BuildingDeletionRequest.class));
    }

    @Test
    void reject_WithRequestNotFound_ShouldThrow() {
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq("note");
        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));
        assertEquals("Request not found", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void reject_WithNonPendingRequest_ShouldThrow() {
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq("note");
        BuildingDeletionRequest rejectedRequest = BuildingDeletionRequest.builder()
                .id(testRequestId)
                .status(BuildingDeletionStatus.REJECTED)
                .build();

        when(buildingDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.of(rejectedRequest));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));
        assertEquals("Request is not PENDING", exception.getMessage());

        verify(buildingDeletionRequestRepository).findById(testRequestId);
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void approve_WithNullNote_ShouldThrow() {
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq(null);

        assertThrows(NullPointerException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository, unitRepository);
    }

    @Test
    void approve_WithEmptyNote_ShouldThrow() {
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq("");

        assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository, unitRepository);
    }

    @Test
    void approve_WithNoteTooLong_ShouldThrow() {
        String longNote = "A".repeat(501);
        BuildingDeletionApproveReq approveReq = new BuildingDeletionApproveReq(longNote);

        assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.approve(testRequestId, approveReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository, unitRepository);
    }

    @Test
    void reject_WithNullNote_ShouldThrow() {
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq(null);

        assertThrows(NullPointerException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository);
    }

    @Test
    void reject_WithEmptyNote_ShouldThrow() {
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq("");

        assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository);
    }

    @Test
    void reject_WithNoteTooLong_ShouldThrow() {
        String longNote = "A".repeat(501);
        BuildingDeletionRejectReq rejectReq = new BuildingDeletionRejectReq(longNote);

        assertThrows(IllegalArgumentException.class,
                () -> buildingDeletionService.reject(testRequestId, rejectReq, authentication));

        verifyNoInteractions(buildingDeletionRequestRepository, buildingRepository);
    }



}