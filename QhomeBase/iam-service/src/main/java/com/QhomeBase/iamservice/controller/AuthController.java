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

import java.util.List;
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
            log.info("Login success for user={} tenantId={}", loginRequest.username(), loginRequest.tenantId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for user={} tenantId={} reason={}", loginRequest.username(), loginRequest.tenantId(), e.getMessage());
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
            @RequestHeader("X-User-ID") UUID userId,
            @RequestParam UUID tenantId) {
        try {
            authService.refreshToken(userId, tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/tenants")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<List<UUID>> getUserTenants(@PathVariable UUID userId) {
        try {
            List<UUID> tenants = authService.getUserTenants(userId);
            return ResponseEntity.ok(tenants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/tenant/{tenantId}/roles")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<List<String>> getUserRolesInTenant(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        try {
            List<String> roles = authService.getUserRolesInTenant(userId, tenantId);
            return ResponseEntity.ok(roles);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/tenant/{tenantId}/validate")
    @PreAuthorize("@authz.canViewUser(#userId)")
    public ResponseEntity<Boolean> validateUserAccess(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        boolean hasAccess = authService.validateUserAccess(userId, tenantId);
        return ResponseEntity.ok(hasAccess);
    }
}

