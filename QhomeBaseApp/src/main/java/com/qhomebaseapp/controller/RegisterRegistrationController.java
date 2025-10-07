package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
public class RegisterRegistrationController {

    private final RegisterRegistrationService service;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegisterServiceRequestResponseDto> register(
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        RegisterServiceRequestResponseDto result = service.registerService(dto, userId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegisterServiceRequestResponseDto>> getByUser(Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        List<RegisterServiceRequestResponseDto> list = service.getByUserId(userId);

        return ResponseEntity.ok(list);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUserId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in authentication");
    }
}
