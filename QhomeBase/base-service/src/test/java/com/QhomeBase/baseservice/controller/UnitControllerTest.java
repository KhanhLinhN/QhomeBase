package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UnitControllerTest {

    private UnitService service;
    private UnitController controller;

    @BeforeEach
    void setup() {
        service = mock(UnitService.class);
        controller = new UnitController(service);
    }

    @Test
    void createUnit_ok() {
        when(service.createUnit(any(UnitCreateDto.class))).thenReturn(mock(UnitDto.class));
        ResponseEntity<UnitDto> resp = controller.createUnit(mock(UnitCreateDto.class), null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).createUnit(any(UnitCreateDto.class));
    }

    @Test
    void createUnit_illegalState_returnsBadRequest() {
        when(service.createUnit(any(UnitCreateDto.class))).thenThrow(new IllegalStateException("bad"));
        ResponseEntity<UnitDto> resp = controller.createUnit(mock(UnitCreateDto.class), null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void createUnit_unhandledException_propagates() {
        when(service.createUnit(any(UnitCreateDto.class))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> controller.createUnit(mock(UnitCreateDto.class), null));
    }

    @Test
    void createUnit_illegalArgument_returnsBadRequest() {
        when(service.createUnit(any(UnitCreateDto.class))).thenThrow(new IllegalArgumentException("bad"));
        ResponseEntity<UnitDto> resp = controller.createUnit(mock(UnitCreateDto.class), null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void updateUnit_ok() {
        UUID id = UUID.randomUUID();
        when(service.updateUnit(any(UnitUpdateDto.class), eq(id))).thenReturn(mock(UnitDto.class));
        ResponseEntity<UnitDto> resp = controller.updateUnit(id, mock(UnitUpdateDto.class));
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).updateUnit(any(UnitUpdateDto.class), eq(id));
    }

    @Test
    void updateUnit_illegalArgument_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(service.updateUnit(any(UnitUpdateDto.class), eq(id))).thenThrow(new IllegalArgumentException("nf"));
        ResponseEntity<UnitDto> resp = controller.updateUnit(id, mock(UnitUpdateDto.class));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void updateUnit_unhandledException_propagates() {
        UUID id = UUID.randomUUID();
        when(service.updateUnit(any(UnitUpdateDto.class), eq(id))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> controller.updateUnit(id, mock(UnitUpdateDto.class)));
    }

    @Test
    void deleteUnit_ok() {
        UUID id = UUID.randomUUID();
        doNothing().when(service).deleteUnit(eq(id));
        ResponseEntity<Void> resp = controller.deleteUnit(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).deleteUnit(eq(id));
    }

    @Test
    void deleteUnit_illegalArgument_returnsNotFound() {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("nf")).when(service).deleteUnit(eq(id));
        ResponseEntity<Void> resp = controller.deleteUnit(id);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getUnitById_ok() {
        UUID id = UUID.randomUUID();
        when(service.getUnitById(eq(id))).thenReturn(mock(UnitDto.class));
        ResponseEntity<UnitDto> resp = controller.getUnitById(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getUnitById(eq(id));
    }

    @Test
    void getUnitById_illegalArgument_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(service.getUnitById(eq(id))).thenThrow(new IllegalArgumentException("nf"));
        ResponseEntity<UnitDto> resp = controller.getUnitById(id);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getUnitById_unhandledException_propagates() {
        UUID id = UUID.randomUUID();
        when(service.getUnitById(eq(id))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> controller.getUnitById(id));
    }

    @Test
    void getUnitsByBuildingId_ok() {
        UUID bid = UUID.randomUUID();
        when(service.getUnitsByBuildingId(eq(bid))).thenReturn(List.of(mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByBuildingId(bid);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getUnitsByBuildingId(eq(bid));
    }

    @Test
    void getUnitsByBuildingId_returnsListSize() {
        UUID bid = UUID.randomUUID();
        when(service.getUnitsByBuildingId(eq(bid))).thenReturn(List.of(mock(UnitDto.class), mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByBuildingId(bid);
        assertEquals(2, resp.getBody().size());
    }

    @Test
    void getUnitsByTenantId_ok() {
        UUID tid = UUID.randomUUID();
        when(service.getUnitsByTenantId(eq(tid))).thenReturn(List.of(mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByTenantId(tid);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getUnitsByTenantId(eq(tid));
    }

    @Test
    void getUnitsByTenantId_returnsListSize() {
        UUID tid = UUID.randomUUID();
        when(service.getUnitsByTenantId(eq(tid)))
                .thenReturn(List.of(mock(UnitDto.class), mock(UnitDto.class), mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByTenantId(tid);
        assertEquals(3, resp.getBody().size());
    }

    @Test
    void getUnitsByFloor_ok() {
        UUID bid = UUID.randomUUID();
        when(service.getUnitsByFloor(eq(bid), eq(5))).thenReturn(List.of(mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByFloor(bid, 5);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getUnitsByFloor(eq(bid), eq(5));
    }

    @Test
    void getUnitsByFloor_callsServiceWithParams() {
        UUID bid = UUID.randomUUID();
        controller.getUnitsByFloor(bid, 8);
        verify(service).getUnitsByFloor(eq(bid), eq(8));
    }

    @Test
    void deleteUnit_unhandledException_propagates() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(service).deleteUnit(eq(id));
        assertThrows(RuntimeException.class, () -> controller.deleteUnit(id));
    }

    @Test
    void changeUnitStatus_ok() {
        UUID id = UUID.randomUUID();
        doNothing().when(service).changeUnitStatus(eq(id), eq(UnitStatus.ACTIVE));
        ResponseEntity<Void> resp = controller.changeUnitStatus(id, UnitStatus.ACTIVE);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).changeUnitStatus(eq(id), eq(UnitStatus.ACTIVE));
    }

    @Test
    void changeUnitStatus_illegalArgument_returnsNotFound() {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("nf")).when(service).changeUnitStatus(eq(id), any(UnitStatus.class));
        ResponseEntity<Void> resp = controller.changeUnitStatus(id, UnitStatus.INACTIVE);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void changeUnitStatus_unhandledException_propagates() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(service).changeUnitStatus(eq(id), any(UnitStatus.class));
        assertThrows(RuntimeException.class, () -> controller.changeUnitStatus(id, UnitStatus.INACTIVE));
    }

    @Test
    void createUnit_passesDtoToService() {
        UnitCreateDto dto = mock(UnitCreateDto.class);
        when(service.createUnit(eq(dto))).thenReturn(mock(UnitDto.class));
        controller.createUnit(dto, null);
        verify(service).createUnit(eq(dto));
    }

    @Test
    void updateUnit_passesDtoAndId() {
        UnitUpdateDto dto = mock(UnitUpdateDto.class);
        UUID id = UUID.randomUUID();
        when(service.updateUnit(eq(dto), eq(id))).thenReturn(mock(UnitDto.class));
        controller.updateUnit(id, dto);
        verify(service).updateUnit(eq(dto), eq(id));
    }

    @Test
    void changeUnitStatus_passesStatusToService() {
        UUID id = UUID.randomUUID();
        controller.changeUnitStatus(id, UnitStatus.INACTIVE);
        verify(service).changeUnitStatus(eq(id), eq(UnitStatus.INACTIVE));
    }
}