package com.qhomebaseapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qhomebaseapp.config.TestSecurityConfig;
import com.qhomebaseapp.dto.user.LoginRequestDto;
import com.qhomebaseapp.dto.token.TokenRefreshRequest;
import com.qhomebaseapp.exception.GlobalExceptionHandler;
import com.qhomebaseapp.exception.TokenRefreshException;
import com.qhomebaseapp.model.RefreshToken;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.service.security.LoginAttemptService;
import com.qhomebaseapp.service.token.RefreshTokenService;
import com.qhomebaseapp.service.user.EmailService;
import com.qhomebaseapp.service.user.UserService;
import com.qhomebaseapp.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private UserService userService;
    @MockBean
    private EmailService emailService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private RefreshTokenService refreshTokenService;
    @MockBean
    private LoginAttemptService loginAttemptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================= LOGIN TESTS =============================

    private LoginRequestDto buildLoginRequest(String email, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    @Test
    void testLoginSuccess() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        String password = "Quyetpt2004!";
        String deviceId = "device123";

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail(email);
        mockUser.setRole("RESIDENT");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("refresh-token-value");

        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));
        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token-value");
        Mockito.when(refreshTokenService.createRefreshToken(mockUser, deviceId)).thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", deviceId)
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.role").value("RESIDENT"));
    }

    @Test
    void testLoginBlocked() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "anyPassword"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Try later."));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginUserNotFoundAfterAuth() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));
        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "anyPassword"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User not found after authentication"));
    }

    @Test
    void testLoginMissingEmail() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setPassword("Quyetpt2004!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginMissingPassword() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("minhtrihoangngoc@gmail.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginInvalidEmailFormat() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("minhtrihoangngocmail.com");
        dto.setPassword("Quyetpt2004!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }


    @Test
    void testLoginMissingBothFields() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginMissingDeviceIdHeader() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        String password = "Quyetpt2004!";

        User mockUser = new User();
        mockUser.setId(2L);
        mockUser.setEmail(email);
        mockUser.setRole("MANAGER");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("refresh-token-nodevice");

        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));
        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token-nodevice");
        Mockito.when(refreshTokenService.createRefreshToken(mockUser, "default")).thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-nodevice"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-nodevice"))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void testLoginWithEmptyDeviceIdHeader() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        String password = "Quyetpt2004!";

        User mockUser = new User();
        mockUser.setId(3L);
        mockUser.setEmail(email);
        mockUser.setRole("ADMIN");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("refresh-token-empty");

        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));
        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token-empty");
        Mockito.when(refreshTokenService.createRefreshToken(mockUser, "default")).thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-empty"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-empty"))
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void testLoginInternalAuthError() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any()))
                .thenThrow(new InternalAuthenticationServiceException("Auth provider error"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "device123")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginRefreshTokenCreationError() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));

        User mockUser = new User();
        mockUser.setId(10L);
        mockUser.setEmail(email);
        mockUser.setRole("USER");

        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token-fail");
        Mockito.when(refreshTokenService.createRefreshToken(any(), any()))
                .thenThrow(new RuntimeException("Failed to create refresh token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "deviceX")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "password"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to create refresh token"));
    }

    @Test
    void testLoginJwtGenerationError() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(false);
        Mockito.when(authenticationManager.authenticate(any())).thenReturn(Mockito.mock(Authentication.class));

        User mockUser = new User();
        mockUser.setId(11L);
        mockUser.setEmail(email);
        mockUser.setRole("USER");

        Mockito.when(userService.getUserByEmail(email)).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser))
                .thenThrow(new RuntimeException("JWT generation failed"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "deviceY")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "password"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("JWT generation failed"));
    }

    @Test
    void testLoginAfterMultipleFailedAttemptsBlocked() throws Exception {
        String email = "minhtrihoangngoc@gmail.com";
        Mockito.when(loginAttemptService.isBlocked(email)).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "deviceZ")
                        .content(objectMapper.writeValueAsString(buildLoginRequest(email, "wrongPassword"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Try later."));
    }


    private TokenRefreshRequest buildRefreshRequest(String token) {
        TokenRefreshRequest dto = new TokenRefreshRequest();
        dto.setRefreshToken(token);
        return dto;
    }

    @Test
    void testRefreshTokenSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("minhtrihoangngoc@gmail.com");
        user.setRole("USER");

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-refresh");
        oldToken.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh");
        newToken.setUser(user);

        Mockito.when(refreshTokenService.findByToken("old-refresh")).thenReturn(Optional.of(oldToken));
        Mockito.when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
        Mockito.when(refreshTokenService.createRefreshToken(user, "default")).thenReturn(newToken);
        Mockito.when(jwtUtil.generateAccessToken(user)).thenReturn("new-access");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.username").value("minhtrihoangngoc@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testRefreshTokenNotFound() throws Exception {
        Mockito.when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("invalid"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testRefreshTokenExpired() throws Exception {
        User user = new User();
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setUser(user);

        Mockito.when(refreshTokenService.findByToken("expired")).thenReturn(Optional.of(token));
        Mockito.doThrow(new TokenRefreshException("expired", "Refresh token hết hạn"))
                .when(refreshTokenService).verifyExpiration(token);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("expired"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testRefreshTokenWithoutDeviceId() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setRole("USER");

        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh");

        Mockito.when(refreshTokenService.findByToken("refresh")).thenReturn(Optional.of(token));
        Mockito.when(refreshTokenService.verifyExpiration(token)).thenReturn(token);

        Mockito.when(refreshTokenService.createRefreshToken(user, "default")).thenReturn(newToken);
        Mockito.when(jwtUtil.generateAccessToken(user)).thenReturn("new-access");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void testRefreshTokenCreationFailure() throws Exception {
        User user = new User();
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setUser(user);

        Mockito.when(refreshTokenService.findByToken("refresh")).thenReturn(Optional.of(token));
        Mockito.doThrow(new RuntimeException("Failed to create refresh token"))
                .when(refreshTokenService).createRefreshToken(any(), anyString());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("refresh"))))
                .andExpect(status().is5xxServerError());
    }


    @Test
    void testGenerateAccessTokenFailure() throws Exception {
        User user = new User();
        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh");

        Mockito.when(refreshTokenService.findByToken("refresh")).thenReturn(Optional.of(token));
        Mockito.when(refreshTokenService.verifyExpiration(token)).thenReturn(token);

        Mockito.when(refreshTokenService.createRefreshToken(user, "default")).thenReturn(newToken);

        Mockito.when(jwtUtil.generateAccessToken(user))
                .thenThrow(new RuntimeException("JWT generation failed"));

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("refresh"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("JWT generation failed"));
    }

    @Test
    void testRefreshTokenWithDeviceIdHeader() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setEmail("manager@example.com");
        user.setRole("MANAGER");

        RefreshToken token = new RefreshToken();
        token.setToken("refresh");
        token.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh-manager");

        Mockito.when(refreshTokenService.findByToken("refresh")).thenReturn(Optional.of(token));
        Mockito.when(refreshTokenService.verifyExpiration(token)).thenReturn(token);

        Mockito.when(refreshTokenService.createRefreshToken(user, "deviceABC")).thenReturn(newToken);
        Mockito.when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-manager");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .header("X-Device-Id", "deviceABC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-manager"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-manager"))
                .andExpect(jsonPath("$.username").value("manager@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void testRefreshTokenWithEmptyDeviceIdHeader() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmail("minhtrihoangngoc@gmail.com");
        user.setRole("USER");

        RefreshToken token = new RefreshToken();
        token.setToken("refresh-empty");
        token.setUser(user);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-refresh-empty");

        Mockito.when(refreshTokenService.findByToken("refresh-empty")).thenReturn(Optional.of(token));
        Mockito.when(refreshTokenService.verifyExpiration(token)).thenReturn(token);

        Mockito.when(refreshTokenService.createRefreshToken(user, "default")).thenReturn(newToken);
        Mockito.when(jwtUtil.generateAccessToken(user)).thenReturn("access-empty");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .header("X-Device-Id", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRefreshRequest("refresh-empty"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-empty"))
                .andExpect(jsonPath("$.accessToken").value("access-empty"))
                .andExpect(jsonPath("$.username").value("minhtrihoangngoc@gmail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }


    @Test
    void testRefreshTokenMissingBody() throws Exception {
        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRefreshTokenMissingField() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest();

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(refreshTokenService, jwtUtil);
    }

}

