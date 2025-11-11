package com.QhomeBase.iamservice;

import com.QhomeBase.iamservice.client.BaseServiceClient;
import com.QhomeBase.iamservice.controller.UserController;
import com.QhomeBase.iamservice.dto.UserAccountDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import com.QhomeBase.iamservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserService userService;

    @Mock
    private BaseServiceClient baseServiceClient;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .passwordHash("hash")
                .active(true)
                .roles(new ArrayList<>())
                .build();
    }

    // getUserInfo tests

    @Test
    @DisplayName("getUserInfo: shouldReturn200WithRolesAndPermissions_whenUserExists")
    void getUserInfo_shouldReturn200WithRolesAndPermissions_whenUserExists() {
        // Arrange
        sampleUser.setRoles(List.of(UserRole.ADMIN, UserRole.ACCOUNTANT));
        when(userService.findUserWithRolesById(userId)).thenReturn(Optional.of(sampleUser));
        when(rolePermissionRepository.findPermissionCodesByRole("ADMIN")).thenReturn(List.of("perm1", "perm2"));
        when(rolePermissionRepository.findPermissionCodesByRole("ACCOUNTANT")).thenReturn(List.of("perm2", "perm3"));

        // Act
        ResponseEntity<UserInfoDto> response = userController.getUserInfo(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        UserInfoDto body = response.getBody();
        assertNotNull(body);
        assertEquals(userId.toString(), body.userId());
        assertEquals("john", body.username());
        assertTrue(body.roles().contains("admin"));
        assertTrue(body.roles().contains("accountant"));
        // deduped permissions from set
        assertEquals(Set.of("perm1", "perm2", "perm3"), new HashSet<>(body.permissions()));
        verify(userService).findUserWithRolesById(userId);
        verify(rolePermissionRepository).findPermissionCodesByRole("ADMIN");
        verify(rolePermissionRepository).findPermissionCodesByRole("ACCOUNTANT");
    }

    @Test
    @DisplayName("getUserInfo: shouldReturn200WithEmptyRolesAndPermissions_whenUserHasNoRoles")
    void getUserInfo_shouldReturn200WithEmptyRolesAndPermissions_whenUserHasNoRoles() {
        // Arrange
        sampleUser.setRoles(Collections.emptyList());
        when(userService.findUserWithRolesById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        ResponseEntity<UserInfoDto> response = userController.getUserInfo(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        UserInfoDto body = response.getBody();
        assertNotNull(body);
        assertTrue(body.roles().isEmpty());
        assertTrue(body.permissions().isEmpty());
        verify(userService).findUserWithRolesById(userId);
        verifyNoInteractions(rolePermissionRepository);
    }

    @Test
    @DisplayName("getUserInfo: shouldReturn404_whenUserNotFound")
    void getUserInfo_shouldReturn404_whenUserNotFound() {
        // Arrange
        when(userService.findUserWithRolesById(userId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserInfoDto> response = userController.getUserInfo(userId);

        // Assert
        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(userService).findUserWithRolesById(userId);
    }

    @Test
    @DisplayName("getUserInfo: shouldHandleDuplicatePermissionsAndReturnSetSize")
    void getUserInfo_shouldHandleDuplicatePermissionsAndReturnSetSize() {
        // Arrange
        sampleUser.setRoles(List.of(UserRole.ADMIN, UserRole.ADMIN));
        when(userService.findUserWithRolesById(userId)).thenReturn(Optional.of(sampleUser));
        when(rolePermissionRepository.findPermissionCodesByRole("ADMIN"))
                .thenReturn(List.of("permA", "permA", "permB"));

        // Act
        ResponseEntity<UserInfoDto> response = userController.getUserInfo(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        UserInfoDto body = response.getBody();
        assertNotNull(body);
        assertEquals(Set.of("permA", "permB"), new HashSet<>(body.permissions()));
        verify(rolePermissionRepository, times(2)).findPermissionCodesByRole("ADMIN");
    }

    @Test
    @DisplayName("getUserInfo: shouldReturnEmptyPermissions_whenRoleRepoReturnsNullOrEmpty")
    void getUserInfo_shouldReturnEmptyPermissions_whenRoleRepoReturnsNullOrEmpty() {
        // Arrange
        sampleUser.setRoles(List.of(UserRole.SUPPORTER));
        when(userService.findUserWithRolesById(userId)).thenReturn(Optional.of(sampleUser));
        when(rolePermissionRepository.findPermissionCodesByRole("SUPPORTER")).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<UserInfoDto> response = userController.getUserInfo(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().permissions().isEmpty());
    }

    // getUserStatus tests

    @Test
    @DisplayName("getUserStatus: shouldReturn200WithStatus_whenUserFound")
    void getUserStatus_shouldReturn200WithStatus_whenUserFound() {
        // Arrange
        sampleUser.setActive(true);
        sampleUser.setFailedLoginAttempts(2);
        sampleUser.setAccountLockedUntil(null);
        sampleUser.setLastLogin(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        ResponseEntity<UserController.UserStatusResponse> response = userController.getUserStatus(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        UserController.UserStatusResponse status = response.getBody();
        assertNotNull(status);
        assertTrue(status.active());
        assertEquals(2, status.failedLoginAttempts());
        assertFalse(status.accountLocked());
        assertNotNull(status.lastLogin());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("getUserStatus: shouldIndicateAccountLocked_whenLockedUntilFuture")
    void getUserStatus_shouldIndicateAccountLocked_whenLockedUntilFuture() {
        // Arrange
        sampleUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        ResponseEntity<UserController.UserStatusResponse> response = userController.getUserStatus(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().accountLocked());
    }

    @Test
    @DisplayName("getUserStatus: shouldReturn404_whenUserMissing")
    void getUserStatus_shouldReturn404_whenUserMissing() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserController.UserStatusResponse> response = userController.getUserStatus(userId);

        // Assert
        assertEquals(404, response.getStatusCode().value());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("getUserStatus: shouldHandleNullLastLogin")
    void getUserStatus_shouldHandleNullLastLogin() {
        // Arrange
        sampleUser.setLastLogin(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        ResponseEntity<UserController.UserStatusResponse> response = userController.getUserStatus(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody().lastLogin());
    }
}