package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    private UUID vehicleId;
    private UUID residentId;
    private UUID unitId;
    private VehicleDto sampleVehicle;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        residentId = UUID.randomUUID();
        unitId = UUID.randomUUID();
        sampleVehicle = new VehicleDto(
                vehicleId,
                residentId,
                "John Doe",
                unitId,
                "A-101",
                "30H-12345",
                VehicleKind.CAR,
                "Red",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                UUID.randomUUID(),
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now()
        );
    }

    // createVehicle
    @Test
    @DisplayName("createVehicle: returns 200 OK with created vehicle")
    void createVehicle_success() {
        VehicleCreateDto req = new VehicleCreateDto(residentId, unitId, "30H-12345", VehicleKind.CAR, "Black");
        when(vehicleService.createVehicle(req)).thenReturn(sampleVehicle);

        ResponseEntity<VehicleDto> response = vehicleController.createVehicle(req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(sampleVehicle, response.getBody());
        verify(vehicleService).createVehicle(req);
    }

    @Test
    @DisplayName("createVehicle: invalid input leads to IllegalArgumentException")
    void createVehicle_invalidInput_throws() {
        VehicleCreateDto req = new VehicleCreateDto(residentId, unitId, null, VehicleKind.CAR, "Black");
        when(vehicleService.createVehicle(any())).thenThrow(new IllegalArgumentException("Plate number is required"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> vehicleController.createVehicle(req));
        assertEquals("Plate number is required", ex.getMessage());
        verify(vehicleService).createVehicle(req);
    }

    @Test
    @DisplayName("createVehicle: duplicate plate results in IllegalStateException")
    void createVehicle_duplicatePlate_throws() {
        VehicleCreateDto req = new VehicleCreateDto(residentId, unitId, "30H-12345", VehicleKind.CAR, "Black");
        when(vehicleService.createVehicle(any())).thenThrow(new IllegalStateException("Duplicate plate"));

        assertThrows(IllegalStateException.class, () -> vehicleController.createVehicle(req));
        verify(vehicleService).createVehicle(req);
    }

    @Test
    @DisplayName("createVehicle: access denied propagates")
    void createVehicle_accessDenied_throws() {
        VehicleCreateDto req = new VehicleCreateDto(residentId, unitId, "30H-99999", VehicleKind.CAR, "Blue");
        when(vehicleService.createVehicle(any())).thenThrow(new AccessDeniedException("no access"));

        assertThrows(AccessDeniedException.class, () -> vehicleController.createVehicle(req));
        verify(vehicleService).createVehicle(req);
    }

    @Test
    @DisplayName("createVehicle: service returns null body")
    void createVehicle_nullBody_ok() {
        VehicleCreateDto req = new VehicleCreateDto(residentId, unitId, "30H-88888", VehicleKind.CAR, "White");
        when(vehicleService.createVehicle(any())).thenReturn(null);

        ResponseEntity<VehicleDto> response = vehicleController.createVehicle(req);
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(vehicleService).createVehicle(req);
    }

    // updateVehicle
    @Test
    @DisplayName("updateVehicle: returns 200 OK with updated vehicle")
    void updateVehicle_success() {
        VehicleUpdateDto req = new VehicleUpdateDto(residentId, unitId, "30H-12345", VehicleKind.CAR, "Green", true);
        when(vehicleService.updateVehicle(req, vehicleId)).thenReturn(sampleVehicle);

        ResponseEntity<VehicleDto> response = vehicleController.updateVehicle(vehicleId, req);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(sampleVehicle, response.getBody());
        verify(vehicleService).updateVehicle(req, vehicleId);
    }

    @Test
    @DisplayName("updateVehicle: null id leads to IllegalArgumentException")
    void updateVehicle_nullId_throws() {
        VehicleUpdateDto req = new VehicleUpdateDto(residentId, unitId, "30H-12345", VehicleKind.CAR, "Green", true);
        when(vehicleService.updateVehicle(any(), isNull())).thenThrow(new IllegalArgumentException("Vehicle ID is required"));

        assertThrows(IllegalArgumentException.class, () -> vehicleController.updateVehicle(null, req));
        verify(vehicleService).updateVehicle(req, null);
    }

    @Test
    @DisplayName("updateVehicle: not found propagates IllegalArgumentException")
    void updateVehicle_notFound_throws() {
        VehicleUpdateDto req = new VehicleUpdateDto(residentId, unitId, "30H-99999", VehicleKind.CAR, "Yellow", true);
        when(vehicleService.updateVehicle(any(), any())).thenThrow(new IllegalArgumentException("Vehicle not found"));

        assertThrows(IllegalArgumentException.class, () -> vehicleController.updateVehicle(vehicleId, req));
        verify(vehicleService).updateVehicle(req, vehicleId);
    }

    @Test
    @DisplayName("updateVehicle: access denied propagates")
    void updateVehicle_accessDenied_throws() {
        VehicleUpdateDto req = new VehicleUpdateDto(residentId, unitId, "30H-00001", VehicleKind.CAR, "Silver", true);
        when(vehicleService.updateVehicle(any(), any())).thenThrow(new AccessDeniedException("no access"));

        assertThrows(AccessDeniedException.class, () -> vehicleController.updateVehicle(vehicleId, req));
        verify(vehicleService).updateVehicle(req, vehicleId);
    }

    @Test
    @DisplayName("updateVehicle: service returns null body")
    void updateVehicle_nullBody_ok() {
        VehicleUpdateDto req = new VehicleUpdateDto(residentId, unitId, "30H-43210", VehicleKind.CAR, "Silver", true);
        when(vehicleService.updateVehicle(any(), any())).thenReturn(null);

        ResponseEntity<VehicleDto> response = vehicleController.updateVehicle(vehicleId, req);
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(vehicleService).updateVehicle(req, vehicleId);
    }

    // deleteVehicle
    @Test
    @DisplayName("deleteVehicle: returns 200 OK with empty body")
    void deleteVehicle_success() {
        Mockito.doNothing().when(vehicleService).deleteVehicle(vehicleId);

        ResponseEntity<Void> response = vehicleController.deleteVehicle(vehicleId);
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(vehicleService).deleteVehicle(vehicleId);
    }

    @Test
    @DisplayName("deleteVehicle: null id leads to IllegalArgumentException")
    void deleteVehicle_nullId_throws() {
        Mockito.doThrow(new IllegalArgumentException("Vehicle ID is required")).when(vehicleService).deleteVehicle(null);
        assertThrows(IllegalArgumentException.class, () -> vehicleController.deleteVehicle(null));
        verify(vehicleService).deleteVehicle(null);
    }

    @Test
    @DisplayName("deleteVehicle: not found propagates IllegalArgumentException")
    void deleteVehicle_notFound_throws() {
        Mockito.doThrow(new IllegalArgumentException("Vehicle not found")).when(vehicleService).deleteVehicle(vehicleId);
        assertThrows(IllegalArgumentException.class, () -> vehicleController.deleteVehicle(vehicleId));
        verify(vehicleService).deleteVehicle(vehicleId);
    }

    @Test
    @DisplayName("deleteVehicle: access denied propagates")
    void deleteVehicle_accessDenied_throws() {
        Mockito.doThrow(new AccessDeniedException("no access")).when(vehicleService).deleteVehicle(vehicleId);
        assertThrows(AccessDeniedException.class, () -> vehicleController.deleteVehicle(vehicleId));
        verify(vehicleService).deleteVehicle(vehicleId);
    }

    @Test
    @DisplayName("deleteVehicle: ensures only delete service is invoked")
    void deleteVehicle_onlyDeleteInvoked() {
        Mockito.doNothing().when(vehicleService).deleteVehicle(vehicleId);
        vehicleController.deleteVehicle(vehicleId);
        verify(vehicleService).deleteVehicle(vehicleId);
        Mockito.verifyNoMoreInteractions(vehicleService);
    }

    // getVehicleById
    @Test
    @DisplayName("getVehicleById: returns 200 OK with body")
    void getVehicleById_success() {
        when(vehicleService.getVehicleById(vehicleId)).thenReturn(sampleVehicle);
        ResponseEntity<VehicleDto> response = vehicleController.getVehicleById(vehicleId);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(sampleVehicle, response.getBody());
        verify(vehicleService).getVehicleById(vehicleId);
    }

    @Test
    @DisplayName("getVehicleById: null id leads to IllegalArgumentException")
    void getVehicleById_nullId_throws() {
        when(vehicleService.getVehicleById(null)).thenThrow(new IllegalArgumentException("Vehicle ID is required"));
        assertThrows(IllegalArgumentException.class, () -> vehicleController.getVehicleById(null));
        verify(vehicleService).getVehicleById(null);
    }

    @Test
    @DisplayName("getVehicleById: not found propagates IllegalArgumentException")
    void getVehicleById_notFound_throws() {
        when(vehicleService.getVehicleById(vehicleId)).thenThrow(new IllegalArgumentException("Vehicle not found"));
        assertThrows(IllegalArgumentException.class, () -> vehicleController.getVehicleById(vehicleId));
        verify(vehicleService).getVehicleById(vehicleId);
    }

    @Test
    @DisplayName("getVehicleById: access denied propagates")
    void getVehicleById_accessDenied_throws() {
        when(vehicleService.getVehicleById(vehicleId)).thenThrow(new AccessDeniedException("no access"));
        assertThrows(AccessDeniedException.class, () -> vehicleController.getVehicleById(vehicleId));
        verify(vehicleService).getVehicleById(vehicleId);
    }

    @Test
    @DisplayName("getVehicleById: verifies returned fields")
    void getVehicleById_verifyFields() {
        VehicleDto dto = sampleVehicle;
        when(vehicleService.getVehicleById(vehicleId)).thenReturn(dto);
        ResponseEntity<VehicleDto> response = vehicleController.getVehicleById(vehicleId);
        VehicleDto body = response.getBody();
        assertNotNull(body);
        assertEquals(vehicleId, body.id());
        assertEquals("30H-12345", body.plateNo());
        assertEquals(VehicleKind.CAR, body.kind());
        verify(vehicleService).getVehicleById(vehicleId);
    }

    // getVehiclesByUnitId
    @Test
    @DisplayName("getVehiclesByUnitId: returns 200 OK with list")
    void getVehiclesByUnitId_success() {
        List<VehicleDto> list = List.of(sampleVehicle);
        when(vehicleService.getVehiclesByUnitId(unitId)).thenReturn(list);
        ResponseEntity<List<VehicleDto>> response = vehicleController.getVehiclesByUnitId(unitId);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(list, response.getBody());
        verify(vehicleService).getVehiclesByUnitId(unitId);
    }

    @Test
    @DisplayName("getVehiclesByUnitId: empty list returns 200 with empty body")
    void getVehiclesByUnitId_emptyList() {
        when(vehicleService.getVehiclesByUnitId(unitId)).thenReturn(new ArrayList<>());
        ResponseEntity<List<VehicleDto>> response = vehicleController.getVehiclesByUnitId(unitId);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(vehicleService).getVehiclesByUnitId(unitId);
    }

    @Test
    @DisplayName("getVehiclesByUnitId: null unit id throws IllegalArgumentException")
    void getVehiclesByUnitId_nullId_throws() {
        when(vehicleService.getVehiclesByUnitId(null)).thenThrow(new IllegalArgumentException("Unit ID is required"));
        assertThrows(IllegalArgumentException.class, () -> vehicleController.getVehiclesByUnitId(null));
        verify(vehicleService).getVehiclesByUnitId(null);
    }

    @Test
    @DisplayName("getVehiclesByUnitId: access denied propagates")
    void getVehiclesByUnitId_accessDenied_throws() {
        when(vehicleService.getVehiclesByUnitId(unitId)).thenThrow(new AccessDeniedException("no access"));
        assertThrows(AccessDeniedException.class, () -> vehicleController.getVehiclesByUnitId(unitId));
        verify(vehicleService).getVehiclesByUnitId(unitId);
    }

    @Test
    @DisplayName("getVehiclesByUnitId: large list returns correctly")
    void getVehiclesByUnitId_largeList() {
        List<VehicleDto> list = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            list.add(new VehicleDto(
                    UUID.randomUUID(),
                    residentId,
                    "Resident " + i,
                    unitId,
                    "A-" + i,
                    "PLATE-" + i,
                    VehicleKind.MOTORBIKE,
                    "Color" + i,
                    true,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    UUID.randomUUID(),
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now()
            ));
        }
        when(vehicleService.getVehiclesByUnitId(unitId)).thenReturn(list);
        ResponseEntity<List<VehicleDto>> response = vehicleController.getVehiclesByUnitId(unitId);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(50, response.getBody().size());
        verify(vehicleService).getVehiclesByUnitId(unitId);
    }

    // changeVehicleStatus
    @Test
    @DisplayName("changeVehicleStatus: returns 200 OK with empty body")
    void changeVehicleStatus_success() {
        Mockito.doNothing().when(vehicleService).changeVehicleStatus(vehicleId, true);
        ResponseEntity<Void> response = vehicleController.changeVehicleStatus(vehicleId, true);
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(vehicleService).changeVehicleStatus(vehicleId, true);
    }

    @Test
    @DisplayName("changeVehicleStatus: null id throws IllegalArgumentException")
    void changeVehicleStatus_nullId_throws() {
        Mockito.doThrow(new IllegalArgumentException("Vehicle ID is required")).when(vehicleService).changeVehicleStatus(null, true);
        assertThrows(IllegalArgumentException.class, () -> vehicleController.changeVehicleStatus(null, true));
        verify(vehicleService).changeVehicleStatus(null, true);
    }

    @Test
    @DisplayName("changeVehicleStatus: null active throws IllegalArgumentException")
    void changeVehicleStatus_nullActive_throws() {
        Mockito.doThrow(new IllegalArgumentException("Active flag is required")).when(vehicleService).changeVehicleStatus(vehicleId, null);
        assertThrows(IllegalArgumentException.class, () -> vehicleController.changeVehicleStatus(vehicleId, null));
        verify(vehicleService).changeVehicleStatus(vehicleId, null);
    }

    @Test
    @DisplayName("changeVehicleStatus: access denied propagates")
    void changeVehicleStatus_accessDenied_throws() {
        Mockito.doThrow(new AccessDeniedException("no access")).when(vehicleService).changeVehicleStatus(vehicleId, false);
        assertThrows(AccessDeniedException.class, () -> vehicleController.changeVehicleStatus(vehicleId, false));
        verify(vehicleService).changeVehicleStatus(vehicleId, false);
    }

    @Test
    @DisplayName("changeVehicleStatus: toggling to false OK")
    void changeVehicleStatus_toggleFalse_ok() {
        Mockito.doNothing().when(vehicleService).changeVehicleStatus(vehicleId, false);
        ResponseEntity<Void> response = vehicleController.changeVehicleStatus(vehicleId, false);
        assertEquals(200, response.getStatusCode().value());
        verify(vehicleService).changeVehicleStatus(vehicleId, false);
    }
}