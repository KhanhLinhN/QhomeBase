package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.ErrorResponseDto;
import com.QhomeBase.iamservice.dto.LoginRequestDto;
import com.QhomeBase.iamservice.dto.LoginResponseDto;
import com.QhomeBase.iamservice.dto.OtpVerificationRequestDto;
import com.QhomeBase.iamservice.dto.PasswordResetConfirmRequestDto;
import com.QhomeBase.iamservice.dto.PasswordResetRequestDto;
import com.QhomeBase.iamservice.service.AuthService;
import com.QhomeBase.iamservice.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
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

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        try {
            passwordResetService.requestPasswordReset(request.email());
            return ResponseEntity.ok(Map.of("message", "If the email exists, OTP has been sent"));
        } catch (IllegalStateException ex) {
            log.warn("OTP request throttled for email={} reason={}", request.email(), ex.getMessage());
            return ResponseEntity.status(429).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequestDto request) {
        boolean valid = passwordResetService.verifyOtp(request.email(), request.otp());
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        }
        return ResponseEntity.ok(Map.of("message", "OTP valid"));
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmReset(@Valid @RequestBody PasswordResetConfirmRequestDto request) {
        try {
            passwordResetService.resetPassword(request.email(), request.otp(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException ex) {
            log.warn("Password reset failed for email={} reason={}", request.email(), ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
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

