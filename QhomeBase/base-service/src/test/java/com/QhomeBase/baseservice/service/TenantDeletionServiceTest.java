package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.TenantDeletionRequest;
import com.QhomeBase.baseservice.model.TenantDeletionStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
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
class TenantDeletionServiceTest {

    @Mock
    private buildingRepository buildingRepository;

    @Mock
    private TenantDeletionRequestRepository tenantDeletionRequestRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private TenantDeletionService tenantDeletionService;

    private UUID testTenantId;
    private UUID testUserId;
    private UUID testTicketId;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTicketId = UUID.randomUUID();
    }

    @Test
    void testChangeStatusOfUnits_ShouldUpdateActiveUnitsToInactive() {
        // Given
        Unit activeUnit1 = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.ACTIVE)
                .build();

        Unit activeUnit2 = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.ACTIVE)
                .build();

        Unit inactiveUnit = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.INACTIVE)
                .build();

        List<Unit> units = Arrays.asList(activeUnit1, activeUnit2, inactiveUnit);

        when(unitRepository.findAllByTenantId(testTenantId)).thenReturn(units);
        when(unitRepository.saveAll(any())).thenReturn(units);

        // When
        tenantDeletionService.changeStatusOfUnits(testTenantId);

        // Then
        assertEquals(UnitStatus.INACTIVE, activeUnit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, activeUnit2.getStatus());
        assertEquals(UnitStatus.INACTIVE, inactiveUnit.getStatus()); // Không thay đổi vì đã INACTIVE

        verify(unitRepository).findAllByTenantId(testTenantId);
        verify(unitRepository).saveAll(units);
    }

    @Test
    void testApprove_ShouldUpdateBuildingAndUnitStatus() {
        // Given
        TenantDeletionRequest deletionRequest = TenantDeletionRequest.builder()
                .id(testTicketId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Test reason")
                .status(TenantDeletionStatus.PENDING)
                .build();

        building testBuilding = building.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(BuildingStatus.ACTIVE)
                .build();

        Unit testUnit = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.ACTIVE)
                .build();

        when(tenantDeletionRequestRepository.findById(testTicketId)).thenReturn(Optional.of(deletionRequest));
        when(buildingRepository.findAllByTenantIdOrderByCodeAsc(testTenantId)).thenReturn(Arrays.asList(testBuilding));
        when(unitRepository.findAllByTenantId(testTenantId)).thenReturn(Arrays.asList(testUnit));
        when(unitRepository.saveAll(any())).thenReturn(Arrays.asList(testUnit));
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(tenantDeletionRequestRepository.save(any())).thenReturn(deletionRequest);

        // When
        var result = tenantDeletionService.approve(testTicketId, "Approved", authentication);

        // Then
        assertNotNull(result);
        assertEquals(TenantDeletionStatus.APPROVED, deletionRequest.getStatus());
        assertEquals(BuildingStatus.PENDING_DELETION, testBuilding.getStatus());
        assertEquals(UnitStatus.INACTIVE, testUnit.getStatus());

        verify(buildingRepository).findAllByTenantIdOrderByCodeAsc(testTenantId);
        verify(unitRepository).findAllByTenantId(testTenantId);
        verify(unitRepository).saveAll(any());
        verify(tenantDeletionRequestRepository).save(deletionRequest);
    }
}





