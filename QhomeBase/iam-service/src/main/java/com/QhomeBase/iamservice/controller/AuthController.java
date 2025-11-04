package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.ErrorResponseDto;
import com.QhomeBase.iamservice.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        try {
            LoginResponseDto response = authService.login(loginRequest);
            log.info("Login success for user={}", loginRequest.username());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for user={} reason={}", loginRequest.username(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("@authz.canLogout()")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-ID") UUID userId) {
        try {
            authService.logout(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("@authz.canRefreshToken()")
    public ResponseEntity<Void> refreshToken(
            @RequestHeader("X-User-ID") UUID userId) {
        try {
            authService.refreshToken(userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
