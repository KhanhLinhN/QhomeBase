package com.QhomeBase.iamservice;

import com.QhomeBase.iamservice.controller.UserPermissionController;
import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.service.UserGrantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPermissionControllerTest {

    @Mock
    private UserGrantService userGrantService;

    @InjectMocks
    private UserPermissionController controller;

    private UUID tenantId;
    private UUID userId;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        auth = mock(Authentication.class);
    }

    // grantPermissionsToUser tests
    @Test
    @DisplayName("grantPermissionsToUser: shouldSetIdsAndCallService_thenReturn200")
    void grantPermissionsToUser_shouldSetIdsAndCallService_thenReturn200() {
        // Arrange
        UserPermissionGrantRequest request = UserPermissionGrantRequest.builder()
                .permissionCodes(List.of("iam.read", "iam.write"))
                .expiresAt(Instant.now().plusSeconds(3600))
                .reason("temporary access")
                .build();
        doNothing().when(userGrantService).grantPermissionsToUser(any(UserPermissionGrantRequest.class), eq(auth));

        // Act
        ResponseEntity<Void> response = controller.grantPermissionsToUser(tenantId, userId, request, auth);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(tenantId, request.getTenantId());
        assertEquals(userId, request.getUserId());
        ArgumentCaptor<UserPermissionGrantRequest> captor = ArgumentCaptor.forClass(UserPermissionGrantRequest.class);
        verify(userGrantService).grantPermissionsToUser(captor.capture(), eq(auth));
        assertEquals(List.of("iam.read", "iam.write"), captor.getValue().getPermissionCodes());
    }

    @Test
    @DisplayName("grantPermissionsToUser: shouldAllowEmptyPermissionsList_andStillReturn200")
    void grantPermissionsToUser_shouldAllowEmptyPermissionsList_andStillReturn200() {
        // Arrange
        UserPermissionGrantRequest request = UserPermissionGrantRequest.builder()
                .permissionCodes(new ArrayList<>())
                .build();
        doNothing().when(userGrantService).grantPermissionsToUser(any(UserPermissionGrantRequest.class), any());

        // Act
        ResponseEntity<Void> response = controller.grantPermissionsToUser(tenantId, userId, request, auth);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).grantPermissionsToUser(any(UserPermissionGrantRequest.class), eq(auth));
    }

    @Test
    @DisplayName("grantPermissionsToUser: shouldPropagateException_whenServiceThrows")
    void grantPermissionsToUser_shouldPropagateException_whenServiceThrows() {
        // Arrange
        UserPermissionGrantRequest request = UserPermissionGrantRequest.builder().permissionCodes(List.of("x")).build();
        doThrow(new IllegalArgumentException("invalid")).when(userGrantService).grantPermissionsToUser(any(), any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.grantPermissionsToUser(tenantId, userId, request, auth));
        verify(userGrantService).grantPermissionsToUser(any(), eq(auth));
    }

    @Test
    @DisplayName("grantPermissionsToUser: shouldAcceptNullAuthentication_andCallService")
    void grantPermissionsToUser_shouldAcceptNullAuthentication_andCallService() {
        // Arrange
        UserPermissionGrantRequest request = UserPermissionGrantRequest.builder().permissionCodes(List.of("a")).build();
        doNothing().when(userGrantService).grantPermissionsToUser(any(), isNull());

        // Act
        ResponseEntity<Void> response = controller.grantPermissionsToUser(tenantId, userId, request, null);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).grantPermissionsToUser(any(), isNull());
    }

    @Test
    @DisplayName("grantPermissionsToUser: shouldThrowNPE_whenRequestIsNull")
    void grantPermissionsToUser_shouldThrowNPE_whenRequestIsNull() {
        // Arrange
        UserPermissionGrantRequest request = null;

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> controller.grantPermissionsToUser(tenantId, userId, request, auth));
        verifyNoInteractions(userGrantService);
    }

    // denyPermissionsFromUser tests
    @Test
    @DisplayName("denyPermissionsFromUser: shouldSetIdsAndCallService_thenReturn200")
    void denyPermissionsFromUser_shouldSetIdsAndCallService_thenReturn200() {
        // Arrange
        UserPermissionDenyRequest request = UserPermissionDenyRequest.builder()
                .permissionCodes(List.of("iam.delete"))
                .expiresAt(Instant.now().plusSeconds(600))
                .reason("risk mitigation")
                .build();
        doNothing().when(userGrantService).denyPermissionsFromUser(any(UserPermissionDenyRequest.class), eq(auth));

        // Act
        ResponseEntity<Void> response = controller.denyPermissionsFromUser(tenantId, userId, request, auth);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userId, request.getUserId());
        assertEquals(tenantId, request.getTenantId());
        verify(userGrantService).denyPermissionsFromUser(any(UserPermissionDenyRequest.class), eq(auth));
    }

    @Test
    @DisplayName("denyPermissionsFromUser: shouldAllowEmptyList_andReturn200")
    void denyPermissionsFromUser_shouldAllowEmptyList_andReturn200() {
        // Arrange
        UserPermissionDenyRequest request = UserPermissionDenyRequest.builder().permissionCodes(new ArrayList<>())
                .build();
        doNothing().when(userGrantService).denyPermissionsFromUser(any(), any());

        // Act
        ResponseEntity<Void> response = controller.denyPermissionsFromUser(tenantId, userId, request, auth);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).denyPermissionsFromUser(any(), eq(auth));
    }

    @Test
    @DisplayName("denyPermissionsFromUser: shouldPropagateException_whenServiceThrows")
    void denyPermissionsFromUser_shouldPropagateException_whenServiceThrows() {
        // Arrange
        UserPermissionDenyRequest request = UserPermissionDenyRequest.builder().permissionCodes(List.of("x")).build();
        doThrow(new IllegalArgumentException("bad deny")).when(userGrantService).denyPermissionsFromUser(any(), any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> controller.denyPermissionsFromUser(tenantId, userId, request, auth));
        verify(userGrantService).denyPermissionsFromUser(any(), eq(auth));
    }

    @Test
    @DisplayName("denyPermissionsFromUser: shouldAcceptNullAuthentication")
    void denyPermissionsFromUser_shouldAcceptNullAuthentication() {
        // Arrange
        UserPermissionDenyRequest request = UserPermissionDenyRequest.builder().permissionCodes(List.of("a")).build();
        doNothing().when(userGrantService).denyPermissionsFromUser(any(), isNull());

        // Act
        ResponseEntity<Void> response = controller.denyPermissionsFromUser(tenantId, userId, request, null);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).denyPermissionsFromUser(any(), isNull());
    }

    @Test
    @DisplayName("denyPermissionsFromUser: shouldThrowNPE_whenRequestIsNull")
    void denyPermissionsFromUser_shouldThrowNPE_whenRequestIsNull() {
        // Arrange
        UserPermissionDenyRequest request = null;

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> controller.denyPermissionsFromUser(tenantId, userId, request, auth));
        verifyNoInteractions(userGrantService);
    }

    // revokeGrantsFromUser tests
    @Test
    @DisplayName("revokeGrantsFromUser: shouldSetIds_andCallService_thenReturn200")
    void revokeGrantsFromUser_shouldSetIds_andCallService_thenReturn200() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(List.of("iam.read"))
                .build();
        doNothing().when(userGrantService).revokeGrantsFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeGrantsFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userId, request.getUserId());
        assertEquals(tenantId, request.getTenantId());
        verify(userGrantService).revokeGrantsFromUser(any(UserPermissionRevokeRequest.class));
    }

    @Test
    @DisplayName("revokeGrantsFromUser: shouldAllowEmptyCodes_andReturn200")
    void revokeGrantsFromUser_shouldAllowEmptyCodes_andReturn200() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(new ArrayList<>())
                .build();
        doNothing().when(userGrantService).revokeGrantsFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeGrantsFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).revokeGrantsFromUser(any());
    }

    @Test
    @DisplayName("revokeGrantsFromUser: shouldPropagateException_whenServiceThrows")
    void revokeGrantsFromUser_shouldPropagateException_whenServiceThrows() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(List.of("a"))
                .build();
        doThrow(new IllegalArgumentException("bad revoke")).when(userGrantService).revokeGrantsFromUser(any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.revokeGrantsFromUser(tenantId, userId, request));
        verify(userGrantService).revokeGrantsFromUser(any());
    }

    @Test
    @DisplayName("revokeGrantsFromUser: shouldThrowNPE_whenRequestIsNull")
    void revokeGrantsFromUser_shouldThrowNPE_whenRequestIsNull() {
        // Arrange
        UserPermissionRevokeRequest request = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> controller.revokeGrantsFromUser(tenantId, userId, request));
        verifyNoInteractions(userGrantService);
    }

    @Test
    @DisplayName("revokeGrantsFromUser: shouldSetIdsEvenWithNullCodes")
    void revokeGrantsFromUser_shouldSetIdsEvenWithNullCodes() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(null).build();
        doNothing().when(userGrantService).revokeGrantsFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeGrantsFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(tenantId, request.getTenantId());
        assertEquals(userId, request.getUserId());
    }

    // revokeDeniesFromUser tests
    @Test
    @DisplayName("revokeDeniesFromUser: shouldSetIds_andCallService_thenReturn200")
    void revokeDeniesFromUser_shouldSetIds_andCallService_thenReturn200() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder()
                .permissionCodes(List.of("iam.delete")).build();
        doNothing().when(userGrantService).revokeDeniesFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeDeniesFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userId, request.getUserId());
        assertEquals(tenantId, request.getTenantId());
        verify(userGrantService).revokeDeniesFromUser(any());
    }

    @Test
    @DisplayName("revokeDeniesFromUser: shouldAllowEmptyCodes_andReturn200")
    void revokeDeniesFromUser_shouldAllowEmptyCodes_andReturn200() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(new ArrayList<>())
                .build();
        doNothing().when(userGrantService).revokeDeniesFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeDeniesFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userGrantService).revokeDeniesFromUser(any());
    }

    @Test
    @DisplayName("revokeDeniesFromUser: shouldPropagateException_whenServiceThrows")
    void revokeDeniesFromUser_shouldPropagateException_whenServiceThrows() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(List.of("a"))
                .build();
        doThrow(new IllegalArgumentException("bad revoke")).when(userGrantService).revokeDeniesFromUser(any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.revokeDeniesFromUser(tenantId, userId, request));
        verify(userGrantService).revokeDeniesFromUser(any());
    }

    @Test
    @DisplayName("revokeDeniesFromUser: shouldThrowNPE_whenRequestIsNull")
    void revokeDeniesFromUser_shouldThrowNPE_whenRequestIsNull() {
        // Arrange
        UserPermissionRevokeRequest request = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> controller.revokeDeniesFromUser(tenantId, userId, request));
        verifyNoInteractions(userGrantService);
    }

    @Test
    @DisplayName("revokeDeniesFromUser: shouldSetIdsEvenWithNullCodes")
    void revokeDeniesFromUser_shouldSetIdsEvenWithNullCodes() {
        // Arrange
        UserPermissionRevokeRequest request = UserPermissionRevokeRequest.builder().permissionCodes(null).build();
        doNothing().when(userGrantService).revokeDeniesFromUser(any());

        // Act
        ResponseEntity<Void> response = controller.revokeDeniesFromUser(tenantId, userId, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(tenantId, request.getTenantId());
        assertEquals(userId, request.getUserId());
    }

    // getUserPermissionSummary tests
    @Test
    @DisplayName("getUserPermissionSummary: shouldReturn200WithSummary_whenServiceReturnsData")
    void getUserPermissionSummary_shouldReturn200WithSummary_whenServiceReturnsData() {
        // Arrange
        UserPermissionSummaryDto summary = UserPermissionSummaryDto.builder()
                .userId(userId)
                .tenantId(tenantId)
                .grants(List.of(UserPermissionOverrideDto.builder().permissionCode("a").granted(true).build()))
                .denies(List.of(UserPermissionOverrideDto.builder().permissionCode("b").granted(false).build()))
                .totalGrants(1)
                .totalDenies(1)
                .activeGrants(1)
                .activeDenies(0)
                .effectivePermissions(List.of("a"))
                .totalEffectivePermissions(1)
                .build();
        when(userGrantService.getUserPermissionSummary(userId, tenantId)).thenReturn(summary);

        // Act
        ResponseEntity<UserPermissionSummaryDto> response = controller.getUserPermissionSummary(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary, response.getBody());
        verify(userGrantService).getUserPermissionSummary(userId, tenantId);
    }

    @Test
    @DisplayName("getUserPermissionSummary: shouldPropagateException_whenServiceThrows")
    void getUserPermissionSummary_shouldPropagateException_whenServiceThrows() {
        // Arrange
        when(userGrantService.getUserPermissionSummary(userId, tenantId))
                .thenThrow(new IllegalArgumentException("bad args"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.getUserPermissionSummary(tenantId, userId));
    }

    @Test
    @DisplayName("getUserPermissionSummary: shouldReturn200WithNullBody_whenServiceReturnsNull")
    void getUserPermissionSummary_shouldReturn200WithNullBody_whenServiceReturnsNull() {
        // Arrange
        when(userGrantService.getUserPermissionSummary(userId, tenantId)).thenReturn(null);

        // Act
        ResponseEntity<UserPermissionSummaryDto> response = controller.getUserPermissionSummary(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getUserPermissionSummary: shouldWorkWithDifferentIds")
    void getUserPermissionSummary_shouldWorkWithDifferentIds() {
        // Arrange
        UUID otherUser = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UserPermissionSummaryDto summary = UserPermissionSummaryDto.builder().userId(otherUser).tenantId(otherTenant)
                .build();
        when(userGrantService.getUserPermissionSummary(otherUser, otherTenant)).thenReturn(summary);

        // Act
        ResponseEntity<UserPermissionSummaryDto> response = controller.getUserPermissionSummary(otherTenant, otherUser);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(summary, response.getBody());
    }

    @Test
    @DisplayName("getUserPermissionSummary: shouldSupportLargeEffectivePermissions")
    void getUserPermissionSummary_shouldSupportLargeEffectivePermissions() {
        // Arrange
        List<String> manyPerms = new ArrayList<>();
        for (int i = 0; i < 100; i++)
            manyPerms.add("perm" + i);
        UserPermissionSummaryDto summary = UserPermissionSummaryDto.builder().effectivePermissions(manyPerms)
                .totalEffectivePermissions(100).build();
        when(userGrantService.getUserPermissionSummary(userId, tenantId)).thenReturn(summary);

        // Act
        ResponseEntity<UserPermissionSummaryDto> response = controller.getUserPermissionSummary(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(100, response.getBody().getTotalEffectivePermissions());
    }

    // getActiveGrants tests
    @Test
    @DisplayName("getActiveGrants: shouldReturn200WithList")
    void getActiveGrants_shouldReturn200WithList() {
        // Arrange
        when(userGrantService.getActiveGrants(userId, tenantId)).thenReturn(List.of("a", "b"));

        // Act
        ResponseEntity<List<String>> response = controller.getActiveGrants(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("a", "b"), response.getBody());
        verify(userGrantService).getActiveGrants(userId, tenantId);
    }

    @Test
    @DisplayName("getActiveGrants: shouldReturn200WithEmptyList_whenNone")
    void getActiveGrants_shouldReturn200WithEmptyList_whenNone() {
        // Arrange
        when(userGrantService.getActiveGrants(userId, tenantId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<String>> response = controller.getActiveGrants(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("getActiveGrants: shouldPropagateException_whenServiceThrows")
    void getActiveGrants_shouldPropagateException_whenServiceThrows() {
        // Arrange
        when(userGrantService.getActiveGrants(userId, tenantId)).thenThrow(new IllegalArgumentException("bad"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.getActiveGrants(tenantId, userId));
    }

    @Test
    @DisplayName("getActiveGrants: shouldSupportLargeList")
    void getActiveGrants_shouldSupportLargeList() {
        // Arrange
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            codes.add("perm" + i);
        when(userGrantService.getActiveGrants(userId, tenantId)).thenReturn(codes);

        // Act
        ResponseEntity<List<String>> response = controller.getActiveGrants(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(50, response.getBody().size());
    }

    @Test
    @DisplayName("getActiveGrants: shouldHandleDuplicateCodes")
    void getActiveGrants_shouldHandleDuplicateCodes() {
        // Arrange
        when(userGrantService.getActiveGrants(userId, tenantId)).thenReturn(List.of("a", "a", "b"));

        // Act
        ResponseEntity<List<String>> response = controller.getActiveGrants(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("a", "a", "b"), response.getBody()); // controller does not dedupe
    }

    // getActiveDenies tests
    @Test
    @DisplayName("getActiveDenies: shouldReturn200WithList")
    void getActiveDenies_shouldReturn200WithList() {
        // Arrange
        when(userGrantService.getActiveDenies(userId, tenantId)).thenReturn(List.of("x"));

        // Act
        ResponseEntity<List<String>> response = controller.getActiveDenies(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("x"), response.getBody());
        verify(userGrantService).getActiveDenies(userId, tenantId);
    }

    @Test
    @DisplayName("getActiveDenies: shouldReturn200WithEmptyList_whenNone")
    void getActiveDenies_shouldReturn200WithEmptyList_whenNone() {
        // Arrange
        when(userGrantService.getActiveDenies(userId, tenantId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<String>> response = controller.getActiveDenies(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("getActiveDenies: shouldPropagateException_whenServiceThrows")
    void getActiveDenies_shouldPropagateException_whenServiceThrows() {
        // Arrange
        when(userGrantService.getActiveDenies(userId, tenantId)).thenThrow(new IllegalArgumentException("bad"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.getActiveDenies(tenantId, userId));
    }

    @Test
    @DisplayName("getActiveDenies: shouldSupportLargeList")
    void getActiveDenies_shouldSupportLargeList() {
        // Arrange
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 40; i++)
            codes.add("deny" + i);
        when(userGrantService.getActiveDenies(userId, tenantId)).thenReturn(codes);

        // Act
        ResponseEntity<List<String>> response = controller.getActiveDenies(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(40, response.getBody().size());
    }

    @Test
    @DisplayName("getActiveDenies: shouldHandleDuplicateCodes")
    void getActiveDenies_shouldHandleDuplicateCodes() {
        // Arrange
        when(userGrantService.getActiveDenies(userId, tenantId)).thenReturn(List.of("x", "x"));

        // Act
        ResponseEntity<List<String>> response = controller.getActiveDenies(tenantId, userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of("x", "x"), response.getBody());
    }
}