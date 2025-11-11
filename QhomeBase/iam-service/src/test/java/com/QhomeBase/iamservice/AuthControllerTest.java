package com.QhomeBase.iamservice;

import com.QhomeBase.iamservice.controller.AuthController;
import com.QhomeBase.iamservice.dto.ErrorResponseDto;
import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.UserInfoDto;
import com.QhomeBase.iamservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("shouldReturn200AndBody_whenLoginSucceeds")
    void shouldReturn200AndBody_whenLoginSucceeds() {
        // Arrange
        LoginRequestDto request = new LoginRequestDto("john", "password123");
        UserInfoDto userInfo = new UserInfoDto(userId.toString(), "john", "john@example.com", List.of("admin"),
                List.of("perm1"));
        LoginResponseDto resp = new LoginResponseDto("token", "Bearer", 3600L, Instant.now().plusSeconds(3600),
                userInfo);
        when(authService.login(request)).thenReturn(resp);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof LoginResponseDto);
        LoginResponseDto body = (LoginResponseDto) response.getBody();
        assertEquals("token", body.accessToken());
        assertEquals("Bearer", body.tokenType());
        assertEquals(3600L, body.expiresIn());
        assertNotNull(body.expiresAt());
        assertEquals("john", body.userInfo().username());
        verify(authService, times(1)).login(request);
    }

    @Test
    @DisplayName("shouldReturn400WithError_whenLoginFailsWithIllegalArgument")
    void shouldReturn400WithError_whenLoginFailsWithIllegalArgument() {
        // Arrange
        LoginRequestDto request = new LoginRequestDto("john", "wrong");
        when(authService.login(request)).thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponseDto);
        ErrorResponseDto error = (ErrorResponseDto) response.getBody();
        assertEquals("Invalid credentials", error.error());
        verify(authService, times(1)).login(request);
    }

    @Test
    @DisplayName("shouldReturn200_whenRefreshTokenSucceeds")
    void shouldReturn200_whenRefreshTokenSucceeds() {
        // Arrange
        doNothing().when(authService).refreshToken(userId);

        // Act
        ResponseEntity<Void> response = authController.refreshToken(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(authService, times(1)).refreshToken(userId);
    }

    @Test
    @DisplayName("shouldReturn400_whenRefreshTokenFailsWithIllegalArgument")
    void shouldReturn400_whenRefreshTokenFailsWithIllegalArgument() {
        // Arrange
        doThrow(new IllegalArgumentException("User inactive")).when(authService).refreshToken(userId);

        // Act
        ResponseEntity<Void> response = authController.refreshToken(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(authService, times(1)).refreshToken(userId);
    }

    @Test
    @DisplayName("shouldReturn200_whenLogoutSucceeds")
    void shouldReturn200_whenLogoutSucceeds() {
        // Arrange
        doNothing().when(authService).logout(userId);

        // Act
        ResponseEntity<Void> response = authController.logout(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(authService, times(1)).logout(userId);
    }

    @Test
    @DisplayName("shouldReturn400_whenLogoutFailsWithIllegalArgument")
    void shouldReturn400_whenLogoutFailsWithIllegalArgument() {
        // Arrange
        doThrow(new IllegalArgumentException("Unknown user")).when(authService).logout(userId);

        // Act
        ResponseEntity<Void> response = authController.logout(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(authService, times(1)).logout(userId);
    }

    @Test
    @DisplayName("shouldReturn400_whenLoginFailsDueToValidationHandledByService")
    void shouldReturn400_whenLoginFailsDueToValidationHandledByService() {
        // Arrange: simulate Bean Validation errors being translated into
        // IllegalArgumentException by service
        LoginRequestDto invalid = new LoginRequestDto("jo", "123");
        when(authService.login(invalid)).thenThrow(new IllegalArgumentException("Validation failed"));

        // Act
        ResponseEntity<?> response = authController.login(invalid);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        ErrorResponseDto error = (ErrorResponseDto) response.getBody();
        assertEquals("Validation failed", error.error());
        verify(authService, times(1)).login(invalid);
    }

    @Test
    @DisplayName("shouldReturn200WithDifferentTokenType_whenServiceReturnsCustomType")
    void shouldReturn200WithDifferentTokenType_whenServiceReturnsCustomType() {
        // Arrange: ensure controller simply passes through token type from service
        LoginRequestDto request = new LoginRequestDto("alice", "secretSecret");
        UserInfoDto userInfo = new UserInfoDto(userId.toString(), "alice", "alice@example.com", List.of(), List.of());
        LoginResponseDto resp = new LoginResponseDto("opaque", "Opaque", 1200L, Instant.now().plusSeconds(1200),
                userInfo);
        when(authService.login(request)).thenReturn(resp);

        // Act
        ResponseEntity<?> response = authController.login(request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        LoginResponseDto body = (LoginResponseDto) response.getBody();
        assertEquals("Opaque", body.tokenType());
        assertEquals("opaque", body.accessToken());
        verify(authService).login(request);
    }

}