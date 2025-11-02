package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.BuildingDeletionService;
import com.QhomeBase.baseservice.service.BuildingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BuildingControllerTest {

    private BuildingService buildingService;
    private BuildingDeletionService deletionService;
    private AuthzService authzService;
    private buildingController controller;
    private Authentication auth;

    @BeforeEach
    void setup() {
        buildingService = mock(BuildingService.class);
        deletionService = mock(BuildingDeletionService.class);
        authzService = mock(AuthzService.class);
        controller = new buildingController(buildingService, deletionService, authzService);
        auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserPrincipal(UUID.randomUUID(), "tester", UUID.randomUUID(),
                List.of("tenant_manager"), List.of(), "tok"));
    }

    @Test
    void findAll_utcid01_validTenantId_returnsOk() {
        UUID tid = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        when(buildingService.findAllByTenantIdOrderByCodeAsc(eq(tid)))
                .thenReturn(List.of(mock(Building.class)));

        ResponseEntity<List<Building>> resp = controller.findAll(tid);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        verify(buildingService).findAllByTenantIdOrderByCodeAsc(eq(tid));
    }

    @Test
    void findAll_utcid02_nullTenantId_returnsBadRequest() {
        ResponseEntity<List<Building>> resp = controller.findAll(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        verify(buildingService, never()).findAllByTenantIdOrderByCodeAsc(any());
    }

    @Test
    void getBuildingById_utcid01_validId_returnsOk() {
        UUID id = UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75");
        BuildingDto dto = mock(BuildingDto.class);
        when(buildingService.getBuildingById(eq(id))).thenReturn(dto);

        ResponseEntity<BuildingDto> resp = controller.getBuildingById(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(buildingService).getBuildingById(eq(id));
    }
    @Test
    void getBuildingById_utcid05_nonExistentId_returnsBadRequest() {
        UUID notFoundId = UUID.fromString("4b9d3e8f-a1c2-5d67-b8e9-0f1a2b3c4d5e");
        when(buildingService.getBuildingById(eq(notFoundId)))
                .thenThrow(new IllegalArgumentException("Building not found with id: 4b9d3e8f-a1c2-5d67-b8e9-0f1a2b3c4d5e"));

        ResponseEntity<BuildingDto> resp = controller.getBuildingById(notFoundId);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(buildingService).getBuildingById(eq(notFoundId));
    }
    @Test
    void createBuilding_utcid01_validData_returnsOk() {
        UUID tenantId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        BuildingCreateReq req = new BuildingCreateReq(
                "B01",
                "Building A"
        );

        BuildingDto result = mock(BuildingDto.class);
        when(buildingService.createBuilding(any(BuildingCreateReq.class), eq(tenantId), anyString()))
                .thenReturn(result);

        ResponseEntity<BuildingDto> resp = controller.createBuilding(req, tenantId, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(result, resp.getBody());
        verify(buildingService).createBuilding(eq(req), eq(tenantId), anyString());
    }
    

}