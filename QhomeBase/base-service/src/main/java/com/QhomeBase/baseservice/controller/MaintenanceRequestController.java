package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CreateMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.MaintenanceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance-requests")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMaintenanceRequestDto requestDto) {
        try {
            MaintenanceRequestDto created = maintenanceRequestService.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to create maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}

