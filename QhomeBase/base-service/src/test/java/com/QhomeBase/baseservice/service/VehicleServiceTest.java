package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.Vehicle;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.VehicleRepository;
import com.QhomeBase.baseservice.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VehicleServiceTest {

    @Mock 
    private VehicleRepository vehicleRepository;
    
    @Mock 
    private ResidentRepository residentRepository;
    
    @Mock 
    private UnitRepository unitRepository;
    
    @Mock 
    private TenantRepository tenantRepository;

    private VehicleService vehicleService;
    
    private UUID testTenantId;
    private UUID testResidentId;
    private UUID testUnitId;
    private UUID testVehicleId;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, residentRepository, unitRepository, tenantRepository);

        testTenantId = UUID.randomUUID();
        testResidentId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();
        testVehicleId = UUID.randomUUID();
    }

    @Test
    void createVehicle_WithValidData_ShouldReturnVehicleDto() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                "Red"
        );
        
        Resident resident = Resident.builder()
                .id(testResidentId)
                .tenantId(testTenantId)
                .fullName("John Doe")
                .build();

        Unit unit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .code("A101")
                .build();

        Vehicle savedVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .resident(resident)
                .unit(unit)
                .plateNo("ABC123")
                .kind(VehicleKind.CAR)
                .color("Red")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRepository.existsByTenantIdAndPlateNo(testTenantId, "ABC123"))
                .thenReturn(false);
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.of(resident));
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(unit));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        VehicleDto result = vehicleService.createVehicle(request);

        assertNotNull(result);
        assertEquals(testVehicleId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testResidentId, result.residentId());
        assertEquals("John Doe", result.residentName());
        assertEquals(testUnitId, result.unitId());
        assertEquals("A101", result.unitCode());
        assertEquals("ABC123", result.plateNo());
        assertEquals(VehicleKind.CAR, result.kind());
        assertEquals("Red", result.color());
        assertEquals(true, result.active());

        verify(vehicleRepository).existsByTenantIdAndPlateNo(testTenantId, "ABC123");
        verify(residentRepository).findById(testResidentId);
        verify(unitRepository).findById(testUnitId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_WithNullTenantId_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                null,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                "Red"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleService.createVehicle(request));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithNullPlateNo_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                null,
                VehicleKind.CAR,
                "Red"
        );

        assertThrows(NullPointerException.class,
                () -> vehicleService.createVehicle(request));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithEmptyPlateNo_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "",
                VehicleKind.CAR,
                "Red"
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.createVehicle(request));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithPlateNoTooLong_ShouldThrow() {
        String longPlateNo = "A".repeat(21);
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                longPlateNo,
                VehicleKind.CAR,
                "Red"
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.createVehicle(request));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithColorTooLong_ShouldThrow() {
        String longColor = "A".repeat(51);
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                longColor
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.createVehicle(request));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithDuplicatePlateNo_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                "Red"
        );

        when(vehicleRepository.existsByTenantIdAndPlateNo(testTenantId, "ABC123"))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> vehicleService.createVehicle(request));

        verify(vehicleRepository).existsByTenantIdAndPlateNo(testTenantId, "ABC123");
        verifyNoInteractions(residentRepository, unitRepository);
    }

    @Test
    void createVehicle_WithNonExistentResident_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                "Red"
        );

        when(vehicleRepository.existsByTenantIdAndPlateNo(testTenantId, "ABC123"))
                .thenReturn(false);
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.createVehicle(request));

        verify(vehicleRepository).existsByTenantIdAndPlateNo(testTenantId, "ABC123");
        verify(residentRepository).findById(testResidentId);
        verifyNoInteractions(unitRepository);
    }

    @Test
    void createVehicle_WithNonExistentUnit_ShouldThrow() {
        VehicleCreateDto request = new VehicleCreateDto(
                testTenantId,
                testResidentId,
                testUnitId,
                "ABC123",
                VehicleKind.CAR,
                "Red"
        );

        when(vehicleRepository.existsByTenantIdAndPlateNo(testTenantId, "ABC123"))
                .thenReturn(false);
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.of(Resident.builder().build()));
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.createVehicle(request));

        verify(vehicleRepository).existsByTenantIdAndPlateNo(testTenantId, "ABC123");
        verify(residentRepository).findById(testResidentId);
        verify(unitRepository).findById(testUnitId);
    }

    @Test
    void updateVehicle_WithValidData_ShouldReturnVehicleDto() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "XYZ789",
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );
        
        Resident resident = Resident.builder()
                .id(testResidentId)
                .tenantId(testTenantId)
                .fullName("Jane Doe")
                .build();

        Unit unit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .code("B202")
                .build();

        Vehicle existingVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .resident(Resident.builder().id(UUID.randomUUID()).build())
                .unit(Unit.builder().id(UUID.randomUUID()).build())
                .plateNo("ABC123")
                .kind(VehicleKind.CAR)
                .color("Red")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Vehicle savedVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .resident(resident)
                .unit(unit)
                .plateNo("XYZ789")
                .kind(VehicleKind.MOTORBIKE)
                .color("Blue")
                .active(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(existingVehicle));
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.of(resident));
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(unit));
        when(vehicleRepository.existsByTenantIdAndPlateNoAndIdNot(testTenantId, "XYZ789", testVehicleId))
                .thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(savedVehicle);

        VehicleDto result = vehicleService.updateVehicle(request, testVehicleId);

        assertNotNull(result);
        assertEquals(testVehicleId, result.id());
        assertEquals(testTenantId, result.tenantId());
        assertEquals(testResidentId, result.residentId());
        assertEquals("Jane Doe", result.residentName());
        assertEquals(testUnitId, result.unitId());
        assertEquals("B202", result.unitCode());
        assertEquals("XYZ789", result.plateNo());
        assertEquals(VehicleKind.MOTORBIKE, result.kind());
        assertEquals("Blue", result.color());
        assertEquals(false, result.active());

        verify(vehicleRepository).findById(testVehicleId);
        verify(residentRepository).findById(testResidentId);
        verify(unitRepository).findById(testUnitId);
        verify(vehicleRepository).existsByTenantIdAndPlateNoAndIdNot(testTenantId, "XYZ789", testVehicleId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void updateVehicle_WithNullPlateNo_ShouldThrow() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                null,
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        assertThrows(NullPointerException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void updateVehicle_WithEmptyPlateNo_ShouldThrow() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "",
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void updateVehicle_WithPlateNoTooLong_ShouldThrow() {
        String longPlateNo = "A".repeat(21);
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                longPlateNo,
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void updateVehicle_WithColorTooLong_ShouldThrow() {
        String longColor = "A".repeat(51);
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "XYZ789",
                VehicleKind.MOTORBIKE,
                longColor,
                false
        );

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verifyNoInteractions(vehicleRepository, residentRepository, unitRepository);
    }

    @Test
    void updateVehicle_WithDuplicatePlateNo_ShouldThrow() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "XYZ789",
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        Resident resident = Resident.builder()
                .id(testResidentId)
                .tenantId(testTenantId)
                .fullName("John Doe")
                .build();

        Unit unit = Unit.builder()
                .id(testUnitId)
                .tenantId(testTenantId)
                .code("A101")
                .build();

        Vehicle existingVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .build();

        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(existingVehicle));
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.of(resident));
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(unit));
        when(vehicleRepository.existsByTenantIdAndPlateNoAndIdNot(testTenantId, "XYZ789", testVehicleId))
                .thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verify(vehicleRepository).findById(testVehicleId);
        verify(residentRepository).findById(testResidentId);
        verify(unitRepository).findById(testUnitId);
        verify(vehicleRepository).existsByTenantIdAndPlateNoAndIdNot(testTenantId, "XYZ789", testVehicleId);
    }

    @Test
    void updateVehicle_WithNonExistentResident_ShouldThrow() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "XYZ789",
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        Vehicle existingVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .build();

        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(existingVehicle));
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verify(vehicleRepository).findById(testVehicleId);
        verify(residentRepository).findById(testResidentId);
        verify(vehicleRepository, never()).existsByTenantIdAndPlateNoAndIdNot(any(), any(), any());
        verifyNoInteractions(unitRepository);
    }

    @Test
    void updateVehicle_WithNonExistentUnit_ShouldThrow() {
        VehicleUpdateDto request = new VehicleUpdateDto(
                testResidentId,
                testUnitId,
                "XYZ789",
                VehicleKind.MOTORBIKE,
                "Blue",
                false
        );

        Vehicle existingVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .build();

        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(existingVehicle));
        when(residentRepository.findById(testResidentId)).thenReturn(Optional.of(Resident.builder().build()));
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.updateVehicle(request, testVehicleId));

        verify(vehicleRepository).findById(testVehicleId);
        verify(residentRepository).findById(testResidentId);
        verify(unitRepository).findById(testUnitId);
        verify(vehicleRepository, never()).existsByTenantIdAndPlateNoAndIdNot(any(), any(), any());
    }

    @Test
    void deleteVehicle_WithValidId_ShouldSetActiveToFalse() {
        Vehicle existingVehicle = Vehicle.builder()
                .id(testVehicleId)
                .tenantId(testTenantId)
                .plateNo("ABC123")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(vehicleRepository.findById(testVehicleId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(existingVehicle);

        vehicleService.deleteVehicle(testVehicleId);

        assertEquals(false, existingVehicle.getActive());
        assertNotNull(existingVehicle.getUpdatedAt());

        verify(vehicleRepository).findById(testVehicleId);
        verify(vehicleRepository).save(existingVehicle);
    }

    @Test
    void hardDeleteVehicle_WithValidId_ShouldDeleteVehicle() {
        when(vehicleRepository.existsById(testVehicleId)).thenReturn(true);

        vehicleService.hardDeleteVehicle(testVehicleId);

        verify(vehicleRepository).existsById(testVehicleId);
        verify(vehicleRepository).deleteById(testVehicleId);
    }

    @Test
    void hardDeleteVehicle_WithNonExistentId_ShouldThrow() {
        when(vehicleRepository.existsById(testVehicleId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.hardDeleteVehicle(testVehicleId));

        verify(vehicleRepository).existsById(testVehicleId);
        verify(vehicleRepository, never()).deleteById(testVehicleId);
    }


}