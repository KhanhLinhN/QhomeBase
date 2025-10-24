package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.VehicleRegistrationApproveDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationCreateDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationRejectDto;
import com.QhomeBase.baseservice.model.Vehicle;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.model.VehicleRegistrationRequest;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import com.QhomeBase.baseservice.repository.VehicleRegistrationRepository;
import com.QhomeBase.baseservice.repository.VehicleRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VehicleRegistrationServiceTest {

    @Mock 
    private VehicleRegistrationRepository vehicleRegistrationRepository;
    
    @Mock 
    private VehicleRepository vehicleRepository;
    
    @Mock
    private FinanceBillingClient financeBillingClient;
    
    @Mock 
    private Authentication authentication;

    private VehicleRegistrationService vehicleRegistrationService;
    
    private UUID testTenantId;
    private UUID testVehicleId;
    private UUID testRequestId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        vehicleRegistrationService = new VehicleRegistrationService(vehicleRegistrationRepository, vehicleRepository, financeBillingClient);

        testTenantId = UUID.randomUUID();
        testVehicleId = UUID.randomUUID();
        testRequestId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.uid()).thenReturn(testUserId);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
    }

    @Test
    void createRegistrationRequest_WithValidData_ShouldReturnVehicleRegistrationDto() {
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                testTenantId,
                testVehicleId,
                "Need to register vehicle for parking"
        );
        
        Vehicle vehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .kind(VehicleKind.CAR)
                .color("Red")
                .active(true)
                .build();

        VehicleRegistrationRequest savedRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .vehicle(vehicle)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(testUserId)
                .requestedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRegistrationRepository.existsByTenantIdAndVehicleId(testTenantId, testVehicleId))
                .thenReturn(false);
        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRegistrationRepository.save(any(VehicleRegistrationRequest.class))).thenReturn(savedRequest);

        VehicleRegistrationDto result = vehicleRegistrationService.createRegistrationRequest(request, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testVehicleId, result.vehicleId());
        assertEquals("ABC123", result.vehiclePlateNo());
        assertEquals("CAR", result.vehicleKind());
        assertEquals("Red", result.vehicleColor());
        assertEquals("Need to register vehicle for parking", result.reason());
        assertEquals(VehicleRegistrationStatus.PENDING, result.status());
        assertEquals(testUserId, result.requestedBy());

        verify(vehicleRegistrationRepository).existsByTenantIdAndVehicleId(testTenantId, testVehicleId);
        verify(vehicleRepository).findById(testVehicleId);
        verify(vehicleRegistrationRepository).save(any(VehicleRegistrationRequest.class));
    }

    @Test
    void createRegistrationRequest_WithNullTenantId_ShouldThrow() {
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                null,
                testVehicleId,
                "Need to register vehicle for parking"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleRegistrationService.createRegistrationRequest(request, authentication));

        verifyNoInteractions(vehicleRegistrationRepository, vehicleRepository);
    }

    @Test
    void createRegistrationRequest_WithNullVehicleId_ShouldThrow() {
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                testTenantId,
                null,
                "Need to register vehicle for parking"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleRegistrationService.createRegistrationRequest(request, authentication));

        verifyNoInteractions(vehicleRegistrationRepository, vehicleRepository);
    }

    @Test
    void createRegistrationRequest_WithReasonTooLong_ShouldThrow() {
        String longReason = "A".repeat(501);
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                testTenantId,
                testVehicleId,
                longReason
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleRegistrationService.createRegistrationRequest(request, authentication));

        verifyNoInteractions(vehicleRegistrationRepository, vehicleRepository);
    }

    @Test
    void createRegistrationRequest_WithDuplicateVehicle_ShouldThrow() {
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                testTenantId,
                testVehicleId,
                "Need to register vehicle for parking"
        );

        when(vehicleRegistrationRepository.existsByTenantIdAndVehicleId(testTenantId, testVehicleId))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> vehicleRegistrationService.createRegistrationRequest(request, authentication));

        verify(vehicleRegistrationRepository).existsByTenantIdAndVehicleId(testTenantId, testVehicleId);
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void createRegistrationRequest_WithNonExistentVehicle_ShouldThrow() {
        VehicleRegistrationCreateDto request = new VehicleRegistrationCreateDto(
                testTenantId,
                testVehicleId,
                "Need to register vehicle for parking"
        );

        when(vehicleRegistrationRepository.existsByTenantIdAndVehicleId(testTenantId, testVehicleId))
                .thenReturn(false);
        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vehicleRegistrationService.createRegistrationRequest(request, authentication));

        verify(vehicleRegistrationRepository).existsByTenantIdAndVehicleId(testTenantId, testVehicleId);
        verify(vehicleRepository).findById(testVehicleId);
    }

    @Test
    void approveRequest_WithValidData_ShouldReturnVehicleRegistrationDto() {
        VehicleRegistrationApproveDto approveDto = new VehicleRegistrationApproveDto(
                "Approved for parking"
        );
        
        Vehicle vehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .kind(VehicleKind.CAR)
                .color("Red")
                .active(true)
                .build();

        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .vehicle(vehicle)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(UUID.randomUUID())
                .requestedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        VehicleRegistrationRequest savedRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .vehicle(vehicle)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.APPROVED)
                .requestedBy(existingRequest.getRequestedBy())
                .approvedBy(testUserId)
                .note("Approved for parking")
                .requestedAt(existingRequest.getRequestedAt())
                .approvedAt(OffsetDateTime.now())
                .createdAt(existingRequest.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));
        when(vehicleRegistrationRepository.save(any(VehicleRegistrationRequest.class))).thenReturn(savedRequest);

        VehicleRegistrationDto result = vehicleRegistrationService.approveRequest(testRequestId, approveDto, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testVehicleId, result.vehicleId());
        assertEquals("ABC123", result.vehiclePlateNo());
        assertEquals("CAR", result.vehicleKind());
        assertEquals("Red", result.vehicleColor());
        assertEquals("Need to register vehicle for parking", result.reason());
        assertEquals(VehicleRegistrationStatus.APPROVED, result.status());
        assertEquals(testUserId, result.approvedBy());
        assertEquals("Approved for parking", result.note());

        verify(vehicleRegistrationRepository).findById(testRequestId);
        verify(vehicleRegistrationRepository).save(any(VehicleRegistrationRequest.class));
    }

    @Test
    void approveRequest_WithNullApprovedBy_ShouldThrow() {
        VehicleRegistrationApproveDto approveDto = new VehicleRegistrationApproveDto(
                "Approved for parking"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleRegistrationService.approveRequest(testRequestId, approveDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void approveRequest_WithNoteTooLong_ShouldThrow() {
        String longNote = "A".repeat(501);
        VehicleRegistrationApproveDto approveDto = new VehicleRegistrationApproveDto(
                longNote
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleRegistrationService.approveRequest(testRequestId, approveDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void approveRequest_WithNonExistentRequest_ShouldThrow() {
        VehicleRegistrationApproveDto approveDto = new VehicleRegistrationApproveDto(
                "Approved for parking"
        );

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vehicleRegistrationService.approveRequest(testRequestId, approveDto, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void approveRequest_WithNonPendingStatus_ShouldThrow() {
        VehicleRegistrationApproveDto approveDto = new VehicleRegistrationApproveDto(
                "Approved for parking"
        );

        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .status(VehicleRegistrationStatus.APPROVED)
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));

        assertThrows(IllegalStateException.class,
                () -> vehicleRegistrationService.approveRequest(testRequestId, approveDto, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void rejectRequest_WithValidData_ShouldReturnVehicleRegistrationDto() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                "Vehicle not eligible for parking"
        );
        
        Vehicle vehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .kind(VehicleKind.CAR)
                .color("Red")
                .active(true)
                .build();

        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .vehicle(vehicle)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(UUID.randomUUID())
                .requestedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        VehicleRegistrationRequest savedRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .vehicle(vehicle)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.REJECTED)
                .requestedBy(existingRequest.getRequestedBy())
                .approvedBy(testUserId)
                .note("Vehicle not eligible for parking")
                .requestedAt(existingRequest.getRequestedAt())
                .approvedAt(OffsetDateTime.now())
                .createdAt(existingRequest.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));
        when(vehicleRegistrationRepository.save(any(VehicleRegistrationRequest.class))).thenReturn(savedRequest);

        VehicleRegistrationDto result = vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testVehicleId, result.vehicleId());
        assertEquals("ABC123", result.vehiclePlateNo());
        assertEquals("CAR", result.vehicleKind());
        assertEquals("Red", result.vehicleColor());
        assertEquals("Need to register vehicle for parking", result.reason());
        assertEquals(VehicleRegistrationStatus.REJECTED, result.status());
        assertEquals(testUserId, result.approvedBy());
        assertEquals("Vehicle not eligible for parking", result.note());

        verify(vehicleRegistrationRepository).findById(testRequestId);
        verify(vehicleRegistrationRepository).save(any(VehicleRegistrationRequest.class));
    }

    @Test
    void rejectRequest_WithNullRejectedBy_ShouldThrow() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                "Vehicle not eligible for parking"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void rejectRequest_WithNullReason_ShouldThrow() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                null
        );

        assertThrows(NullPointerException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void rejectRequest_WithEmptyReason_ShouldThrow() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                ""
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void rejectRequest_WithReasonTooLong_ShouldThrow() {
        String longReason = "A".repeat(501);
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                longReason
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verifyNoInteractions(vehicleRegistrationRepository);
    }

    @Test
    void rejectRequest_WithNonExistentRequest_ShouldThrow() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                "Vehicle not eligible for parking"
        );

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void rejectRequest_WithNonPendingStatus_ShouldThrow() {
        VehicleRegistrationRejectDto rejectDto = new VehicleRegistrationRejectDto(
                "Vehicle not eligible for parking"
        );

        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .status(VehicleRegistrationStatus.APPROVED)
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));

        assertThrows(IllegalStateException.class,
                () -> vehicleRegistrationService.rejectRequest(testRequestId, rejectDto, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void cancelRequest_WithValidData_ShouldReturnVehicleRegistrationDto() {
        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(testUserId)
                .requestedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        VehicleRegistrationRequest savedRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .reason("Need to register vehicle for parking")
                .status(VehicleRegistrationStatus.CANCELED)
                .requestedBy(testUserId)
                .requestedAt(existingRequest.getRequestedAt())
                .createdAt(existingRequest.getCreatedAt())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));
        when(vehicleRegistrationRepository.save(any(VehicleRegistrationRequest.class))).thenReturn(savedRequest);

        VehicleRegistrationDto result = vehicleRegistrationService.cancelRequest(testRequestId, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals("Need to register vehicle for parking", result.reason());
        assertEquals(VehicleRegistrationStatus.CANCELED, result.status());
        assertEquals(testUserId, result.requestedBy());

        verify(vehicleRegistrationRepository).findById(testRequestId);
        verify(vehicleRegistrationRepository).save(any(VehicleRegistrationRequest.class));
    }

    @Test
    void cancelRequest_WithNonExistentRequest_ShouldThrow() {
        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> vehicleRegistrationService.cancelRequest(testRequestId, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void cancelRequest_WithNonPendingStatus_ShouldThrow() {
        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .status(VehicleRegistrationStatus.APPROVED)
                .requestedBy(testUserId)
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));

        assertThrows(IllegalStateException.class,
                () -> vehicleRegistrationService.cancelRequest(testRequestId, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }

    @Test
    void cancelRequest_WithDifferentRequester_ShouldThrow() {
        VehicleRegistrationRequest existingRequest = VehicleRegistrationRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(UUID.randomUUID())
                .build();

        when(vehicleRegistrationRepository.findById(testRequestId)).thenReturn(Optional.of(existingRequest));

        assertThrows(IllegalStateException.class,
                () -> vehicleRegistrationService.cancelRequest(testRequestId, authentication));

        verify(vehicleRegistrationRepository).findById(testRequestId);
    }


}