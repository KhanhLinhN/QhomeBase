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
    void findAll_missingTenantId_returnsBadRequest() {
        ResponseEntity<List<Building>> resp = controller.findAll(null);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void findAll_ok_returnsList() {
        UUID tid = UUID.randomUUID();
        when(buildingService.findAllByTenantIdOrderByCodeAsc(eq(tid))).thenReturn(List.of(mock(Building.class)));
        ResponseEntity<List<Building>> resp = controller.findAll(tid);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        verify(buildingService).findAllByTenantIdOrderByCodeAsc(eq(tid));
    }

    @Test
    void getBuildingById_ok() {
        UUID id = UUID.randomUUID();
        when(buildingService.getBuildingById(eq(id))).thenReturn(mock(BuildingDto.class));
        ResponseEntity<BuildingDto> resp = controller.getBuildingById(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(buildingService).getBuildingById(eq(id));
    }

    @Test
    void getBuildingById_notFound_returns404() {
        UUID id = UUID.randomUUID();
        when(buildingService.getBuildingById(eq(id))).thenThrow(new IllegalArgumentException("nf"));
        ResponseEntity<BuildingDto> resp = controller.getBuildingById(id);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void getBuildingById_ok_returnsBody() {
        UUID id = UUID.randomUUID();
        BuildingDto dto = mock(BuildingDto.class);
        when(buildingService.getBuildingById(eq(id))).thenReturn(dto);
        ResponseEntity<BuildingDto> resp = controller.getBuildingById(id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
    }

    @Test
    void createBuilding_ok() {
        UUID tenantId = UUID.randomUUID();
        when(buildingService.createBuilding(any(BuildingCreateReq.class), eq(tenantId), anyString()))
                .thenReturn(mock(BuildingDto.class));
        ResponseEntity<BuildingDto> resp = controller.createBuilding(mock(BuildingCreateReq.class), tenantId, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(buildingService).createBuilding(any(BuildingCreateReq.class), eq(tenantId), anyString());
    }

    @Test
    void createBuilding_serviceThrows_unhandled_propagates() {
        UUID tenantId = UUID.randomUUID();
        when(buildingService.createBuilding(any(BuildingCreateReq.class), eq(tenantId), anyString()))
                .thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class,
                () -> controller.createBuilding(mock(BuildingCreateReq.class), tenantId, auth));
    }

    @Test
    void updateBuilding_ok() {
        UUID id = UUID.randomUUID();
        when(buildingService.updateBuilding(eq(id), any(BuildingUpdateReq.class), any(Authentication.class)))
                .thenReturn(mock(BuildingDto.class));
        ResponseEntity<BuildingDto> resp = controller.updateBuilding(id, mock(BuildingUpdateReq.class), auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(buildingService).updateBuilding(eq(id), any(BuildingUpdateReq.class), any(Authentication.class));
    }

    @Test
    void updateBuilding_notFound_returns404() {
        UUID id = UUID.randomUUID();
        when(buildingService.updateBuilding(eq(id), any(BuildingUpdateReq.class), any(Authentication.class)))
                .thenThrow(new RuntimeException("nf"));
        ResponseEntity<BuildingDto> resp = controller.updateBuilding(id, mock(BuildingUpdateReq.class), auth);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void updateBuilding_serviceCalledWithAuth() {
        UUID id = UUID.randomUUID();
        BuildingUpdateReq req = mock(BuildingUpdateReq.class);
        when(buildingService.updateBuilding(eq(id), eq(req), any(Authentication.class)))
                .thenReturn(mock(BuildingDto.class));
        controller.updateBuilding(id, req, auth);
        verify(buildingService).updateBuilding(eq(id), eq(req), any(Authentication.class));
    }

    @Test
    void createBuildingDeletionRequest_ok() {
        UUID bid = UUID.randomUUID();
        when(deletionService.createBuildingDeletionRequest(eq(bid), anyString(), any(Authentication.class)))
                .thenReturn(mock(BuildingDeletionRequestDto.class));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.createBuildingDeletionRequest(
                bid, new BuildingDeletionCreateReq(bid, bid, "reason"), auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).createBuildingDeletionRequest(eq(bid), anyString(), any(Authentication.class));
    }

    @Test
    void createBuildingDeletionRequest_illegalArgument_returnsBadRequest() {
        UUID bid = UUID.randomUUID();
        when(deletionService.createBuildingDeletionRequest(eq(bid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalArgumentException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.createBuildingDeletionRequest(
                bid, new BuildingDeletionCreateReq(bid, bid, "reason"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void createBuildingDeletionRequest_illegalState_returnsBadRequest() {
        UUID bid = UUID.randomUUID();
        when(deletionService.createBuildingDeletionRequest(eq(bid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalStateException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.createBuildingDeletionRequest(
                bid, new BuildingDeletionCreateReq(bid, bid, "reason"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void approveBuildingDeletionRequest_ok() {
        UUID rid = UUID.randomUUID();
        when(deletionService.approveBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenReturn(mock(BuildingDeletionRequestDto.class));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.approveBuildingDeletionRequest(
                rid, new BuildingDeletionApproveReq("note"), auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).approveBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class));
    }

    @Test
    void approveBuildingDeletionRequest_illegalArgument_returnsBadRequest() {
        UUID rid = UUID.randomUUID();
        when(deletionService.approveBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalArgumentException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.approveBuildingDeletionRequest(
                rid, new BuildingDeletionApproveReq("note"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void approveBuildingDeletionRequest_illegalState_returnsBadRequest() {
        UUID rid = UUID.randomUUID();
        when(deletionService.approveBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalStateException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.approveBuildingDeletionRequest(
                rid, new BuildingDeletionApproveReq("note"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void rejectBuildingDeletionRequest_ok() {
        UUID rid = UUID.randomUUID();
        when(deletionService.rejectBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenReturn(mock(BuildingDeletionRequestDto.class));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.rejectBuildingDeletionRequest(
                rid, new BuildingDeletionRejectReq("note"), auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).rejectBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class));
    }

    @Test
    void rejectBuildingDeletionRequest_illegalArgument_returnsBadRequest() {
        UUID rid = UUID.randomUUID();
        when(deletionService.rejectBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalArgumentException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.rejectBuildingDeletionRequest(
                rid, new BuildingDeletionRejectReq("note"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void rejectBuildingDeletionRequest_illegalState_returnsBadRequest() {
        UUID rid = UUID.randomUUID();
        when(deletionService.rejectBuildingDeletionRequest(eq(rid), anyString(), any(Authentication.class)))
                .thenThrow(new IllegalStateException("bad"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.rejectBuildingDeletionRequest(
                rid, new BuildingDeletionRejectReq("note"), auth);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void getPendingBuildingDeletionRequests_ok() {
        when(deletionService.getPendingRequests()).thenReturn(List.of(mock(BuildingDeletionRequestDto.class)));
        ResponseEntity<List<BuildingDeletionRequestDto>> resp = controller.getPendingBuildingDeletionRequests();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).getPendingRequests();
    }

    @Test
    void getBuildingDeletionRequest_ok() {
        UUID rid = UUID.randomUUID();
        when(deletionService.getById(eq(rid))).thenReturn(mock(BuildingDeletionRequestDto.class));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.getBuildingDeletionRequest(rid, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).getById(eq(rid));
    }

    @Test
    void getBuildingDeletionRequest_notFound_returns404() {
        UUID rid = UUID.randomUUID();
        when(deletionService.getById(eq(rid))).thenThrow(new IllegalArgumentException("nf"));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.getBuildingDeletionRequest(rid, auth);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void doBuildingDeletion_ok() {
        UUID bid = UUID.randomUUID();
        doNothing().when(deletionService).doBuildingDeletion(eq(bid), any(Authentication.class));
        ResponseEntity<String> resp = controller.doBuildingDeletion(bid, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).doBuildingDeletion(eq(bid), any(Authentication.class));
    }

    @Test
    void getDeletingBuildings_accessDenied_throws() {
        when(authzService.canViewAllDeleteBuildings()).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.getDeletingBuildings());
    }

    @Test
    void getDeletingBuildings_allowed_ok() {
        when(authzService.canViewAllDeleteBuildings()).thenReturn(true);
        when(deletionService.getDeletingBuildings()).thenReturn(List.of(mock(BuildingDeletionRequestDto.class)));
        List<BuildingDeletionRequestDto> list = controller.getDeletingBuildings();
        assertNotNull(list);
        verify(deletionService).getDeletingBuildings();
    }

    @Test
    void getAllBuildingDeletionRequests_allowed_ok() {
        when(authzService.canViewAllDeleteBuildings()).thenReturn(true);
        when(deletionService.getAllBuildingDeletionRequests())
                .thenReturn(List.of(mock(BuildingDeletionRequestDto.class)));
        List<BuildingDeletionRequestDto> list = controller.getAllBuildingDeletionRequests();
        assertNotNull(list);
        verify(deletionService).getAllBuildingDeletionRequests();
    }

    @Test
    void getMyDeletingBuildings_ok() {
        UUID tid = UUID.randomUUID();
        when(deletionService.getDeletingBuildingsByTenantId(eq(tid)))
                .thenReturn(List.of(mock(BuildingDeletionRequestDto.class)));
        List<BuildingDeletionRequestDto> list = controller.getMyDeletingBuildings(tid, auth);
        assertNotNull(list);
        verify(deletionService).getDeletingBuildingsByTenantId(eq(tid));
    }

    @Test
    void getMyAllBuildingDeletionRequests_ok() {
        UUID tid = UUID.randomUUID();
        when(deletionService.getAllBuildingDeletionRequestsByTenantId(eq(tid)))
                .thenReturn(List.of(mock(BuildingDeletionRequestDto.class)));
        List<BuildingDeletionRequestDto> list = controller.getMyAllBuildingDeletionRequests(tid, auth);
        assertNotNull(list);
        verify(deletionService).getAllBuildingDeletionRequestsByTenantId(eq(tid));
    }

    @Test
    void getMyDeletingBuildingsRaw_ok() {
        UUID tid = UUID.randomUUID();
        when(deletionService.getDeletingBuildingsRawByTenantId(eq(tid))).thenReturn(List.of(mock(Building.class)));
        ResponseEntity<List<Building>> resp = controller.getMyDeletingBuildingsRaw(tid, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).getDeletingBuildingsRawByTenantId(eq(tid));
    }

    @Test
    void getBuildingDeletionTargetsStatus_ok() {
        UUID bid = UUID.randomUUID();
        when(deletionService.getBuildingDeletionTargetsStatus(eq(bid))).thenReturn(Map.of("ok", true));
        ResponseEntity<Map<String, Object>> resp = controller.getBuildingDeletionTargetsStatus(bid, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).getBuildingDeletionTargetsStatus(eq(bid));
    }

    @Test
    void completeBuildingDeletion_ok() {
        UUID rid = UUID.randomUUID();
        when(deletionService.completeBuildingDeletion(eq(rid), any(Authentication.class)))
                .thenReturn(mock(BuildingDeletionRequestDto.class));
        ResponseEntity<BuildingDeletionRequestDto> resp = controller.completeBuildingDeletion(rid, auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(deletionService).completeBuildingDeletion(eq(rid), any(Authentication.class));
    }
}