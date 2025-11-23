package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.CreateMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestConfigDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.MaintenanceRequestMonitor;
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
    private final MaintenanceRequestMonitor maintenanceRequestMonitor;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMaintenanceRequestDto requestDto) {
        try {
            MaintenanceRequestDto created = maintenanceRequestService.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | IllegalStateException ex) {
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

    @PostMapping("/admin/{requestId}/respond")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> respondToMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminMaintenanceResponseDto request) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.respondToRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to respond to maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
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

    @PatchMapping("/admin/{requestId}/complete")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> completeMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        MaintenanceRequestDto dto = maintenanceRequestService.completeRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{requestId}/approve-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> approveResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.approveResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to approve response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{requestId}/reject-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> rejectResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.rejectResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to reject response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> cancelMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        MaintenanceRequestDto dto = maintenanceRequestService.cancelRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{requestId}/resend")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> resendMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.resendRequest(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to resend maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<MaintenanceRequestConfigDto> getConfig() {
        MaintenanceRequestConfigDto config = maintenanceRequestMonitor.getConfig();
        return ResponseEntity.ok(config);
    }
}

