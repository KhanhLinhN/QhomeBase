package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.validation.*;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantDeletionServiceTest {
    
    @Mock 
    private BuildingRepository buildingRepository;
    
    @Mock 
    private TenantDeletionRequestRepository tenantDeletionRequestRepository;
    
    @Mock 
    private UnitRepository unitRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserPrincipal userPrincipal;

    private TenantDeletionService tenantDeletionService;
    private Validator validator;
    
    private UUID testTenantId;
    private UUID testUserId;
    private UUID testRequestId;

    @BeforeEach
    void setUp() {
        tenantDeletionService = new TenantDeletionService(
                buildingRepository, 
                tenantDeletionRequestRepository, 
                unitRepository
        );

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testTenantId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testRequestId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(testUserId);
    }

    @Test
    void create_WithValidData_ShouldSuccess() {
        String reason = "Tenant no longer needed";
        when(tenantDeletionRequestRepository.countPendingRequestsByTenantId(testTenantId)).thenReturn(0L);
        when(tenantDeletionRequestRepository.findDeletedTenant(testTenantId)).thenReturn(0L);
        when(tenantDeletionRequestRepository.save(any(TenantDeletionRequest.class)))
                .thenAnswer(invocation -> {
                    TenantDeletionRequest request = invocation.getArgument(0);
                    request.setId(testRequestId);
                    request.setCreatedAt(OffsetDateTime.now());
                    return request;
                });
        TenantDeletionRequestDTO result = tenantDeletionService.create(testTenantId, reason, authentication);
        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals(reason, result.reason());
        assertEquals(TenantDeletionStatus.PENDING, result.status());
        assertNotNull(result.createdAt());
        verify(tenantDeletionRequestRepository).countPendingRequestsByTenantId(testTenantId);
        verify(tenantDeletionRequestRepository).findDeletedTenant(testTenantId);
        verify(tenantDeletionRequestRepository).save(any(TenantDeletionRequest.class));
    }


    @Test
    void approve_WithValidPendingRequest_ShouldReturnApprovedDTO() {
        String note = "Approved for deletion";
        UUID approverId = UUID.randomUUID();
        TenantDeletionRequest pendingRequest = TenantDeletionRequest.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Test reason")
                .status(TenantDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        when(tenantDeletionRequestRepository.findById(testRequestId))
                .thenReturn(Optional.of(pendingRequest));
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.uid()).thenReturn(approverId);

        Building building1 = Building.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(BuildingStatus.ACTIVE)
                .build();
        Building building2 = Building.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(BuildingStatus.PENDING_DELETION)
                .build();

        Unit unit1 = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.ACTIVE)
                .build();
        Unit unit2 = Unit.builder()
                .id(UUID.randomUUID())
                .tenantId(testTenantId)
                .status(UnitStatus.INACTIVE)
                .build();

        when(buildingRepository.findAllByTenantIdOrderByCodeAsc(testTenantId))
                .thenReturn(List.of(building1, building2));
        when(unitRepository.findAllByTenantId(testTenantId))
                .thenReturn(List.of(unit1, unit2));
        when(unitRepository.saveAll(anyList())).thenReturn(List.of(unit1, unit2));
        when(tenantDeletionRequestRepository.save(any(TenantDeletionRequest.class)))
                .thenReturn(pendingRequest);

        TenantDeletionRequestDTO result = tenantDeletionService.approve(testRequestId, note, authentication);

        assertNotNull(result);
        assertEquals(testRequestId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testUserId, result.requestedBy());
        assertEquals(approverId, result.approvedBy());
        assertEquals(note, result.note());
        assertEquals(TenantDeletionStatus.APPROVED, result.status());
        assertNotNull(result.approvedAt());

        assertEquals(BuildingStatus.PENDING_DELETION, building1.getStatus());
        assertEquals(BuildingStatus.PENDING_DELETION, building2.getStatus());

        assertEquals(UnitStatus.INACTIVE, unit1.getStatus());
        assertEquals(UnitStatus.INACTIVE, unit2.getStatus());

        verify(tenantDeletionRequestRepository).findById(testRequestId);
        verify(buildingRepository).findAllByTenantIdOrderByCodeAsc(testTenantId);
        verify(unitRepository).findAllByTenantId(testTenantId);
        verify(unitRepository).saveAll(anyList());
        verify(tenantDeletionRequestRepository).save(pendingRequest);
    }
    @Test
    void createRequest_WithNoReason_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> tenantDeletionService.create(testTenantId, null, authentication));

        verifyNoInteractions(tenantDeletionRequestRepository);
        verifyNoInteractions(buildingRepository, unitRepository);
    }


    @Test
    void createRequest_ExistingPending_ShouldThrow() {

        when(tenantDeletionRequestRepository.countPendingRequestsByTenantId(testTenantId)).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, 
                () -> tenantDeletionService.create(testTenantId, "test reason", authentication));


        verify(tenantDeletionRequestRepository).countPendingRequestsByTenantId(testTenantId);
        verify(tenantDeletionRequestRepository, never()).save(any());
    }
    @Test
    void createRequest_ReasonTooLong_ShouldThrow() {

        String longReason = "A".repeat(501);
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason(longReason)
                .status(TenantDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have validation violations for reason too long");

        boolean hasReasonViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("reason") );
        assertTrue(hasReasonViolation, "Should have size violation for reason field");
    }

    @Test
    void createRequest_WithNullTenantId_ShouldHaveValidationViolation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(null)
                .requestedBy(testUserId)
                .reason("Valid reason")
                .status(TenantDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have validation violations for null tenantId");
        
        boolean hasTenantIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("tenantId"));
        assertTrue(hasTenantIdViolation, "Should have violation for null tenantId");
    }

    @Test
    void createRequest_WithBlankReason_ShouldHaveValidationViolation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("   ")
                .status(TenantDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have validation violations for blank reason");
        
        boolean hasReasonViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("reason"));
        assertTrue(hasReasonViolation, "Should have violation for blank reason");
    }

    @Test
    void createRequest_WithValidData_ShouldPassValidation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Valid reason for deletion")
                .status(TenantDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should not have any validation violations for valid data");
    }

    @Test
    void approveRequest_WithNoteTooLong_ShouldHaveValidationViolation() {
        String longNote = "A".repeat(501);
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Valid reason")
                .note(longNote)
                .status(TenantDeletionStatus.APPROVED)
                .createdAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have validation violations for note too long");
        
        boolean hasNoteViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("note") && 
                             v.getMessage().contains("size"));
        assertTrue(hasNoteViolation, "Should have size violation for note field");
    }

    @Test
    void approveRequest_WithValidNote_ShouldPassValidation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Valid reason")
                .note("Valid approval note")
                .status(TenantDeletionStatus.APPROVED)
                .createdAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should not have any validation violations for valid note");
    }

    @Test
    void approveRequest_WithNullNote_ShouldPassValidation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Valid reason")
                .note(null)
                .status(TenantDeletionStatus.APPROVED)
                .createdAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should not have validation violations for null note");
    }

    @Test
    void approveRequest_WithEmptyNote_ShouldPassValidation() {
        var req = TenantDeletionRequestDTO.builder()
                .id(testRequestId)
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Valid reason")
                .note("")
                .status(TenantDeletionStatus.APPROVED)
                .createdAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();
        
        Set<ConstraintViolation<TenantDeletionRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should not have validation violations for empty note");
    }

}
