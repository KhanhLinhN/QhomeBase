package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<MaintenanceRequestDto>> getMyMaintenanceRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<MaintenanceRequestDto> requests = maintenanceRequestService.getMyRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<MaintenanceRequestDto>> getPendingMaintenanceRequests() {
        return ResponseEntity.ok(maintenanceRequestService.getPendingRequests());
    }

    @PatchMapping("/admin/{requestId}/approve")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> approveMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        MaintenanceRequestDto dto = maintenanceRequestService.approveRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }
}

