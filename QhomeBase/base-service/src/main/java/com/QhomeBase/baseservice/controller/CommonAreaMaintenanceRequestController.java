package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.CreateCommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.AddProgressNoteDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.CommonAreaMaintenanceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/common-area-maintenance-requests")
@RequiredArgsConstructor
@Slf4j
public class CommonAreaMaintenanceRequestController {

    private final CommonAreaMaintenanceRequestService service;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCommonAreaMaintenanceRequestDto requestDto) {
        try {
            CommonAreaMaintenanceRequestDto created = service.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to create common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CommonAreaMaintenanceRequestDto> requests = service.getMyRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @GetMapping("/admin/in-progress")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getInProgressRequests() {
        return ResponseEntity.ok(service.getInProgressRequests());
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getRequestsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(service.getRequestsByStatus(status));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> getRequestById(@PathVariable UUID requestId) {
        CommonAreaMaintenanceRequestDto request = service.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/admin/{requestId}/respond")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> respondToRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminMaintenanceResponseDto request) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.respondToRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to respond to common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/deny")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> denyRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminServiceRequestActionDto request) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.denyRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to deny common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/complete")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> completeRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        CommonAreaMaintenanceRequestDto dto = service.completeRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{requestId}/approve-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> approveResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.approveResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to approve response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{requestId}/reject-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> rejectResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.rejectResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to reject response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> cancelRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        CommonAreaMaintenanceRequestDto dto = service.cancelRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/admin/{requestId}/assign")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> assignToStaff(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody Map<String, UUID> request) {
        UUID staffId = request.get("staffId");
        if (staffId == null) {
            return ResponseEntity.badRequest().body(null);
        }
        try {
            CommonAreaMaintenanceRequestDto dto = service.assignToStaff(
                    principal.uid(),
                    requestId,
                    staffId
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to assign request to staff: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/admin/{requestId}/add-progress-note")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> addProgressNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AddProgressNoteDto request) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.addProgressNote(
                    principal.uid(),
                    requestId,
                    request.note()
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to add progress note: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
}
