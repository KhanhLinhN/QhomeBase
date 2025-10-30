package com.QhomeBase.baseservice.controller;


import com.QhomeBase.baseservice.dto.ApproveDeletionReq;
import com.QhomeBase.baseservice.dto.CreateDeletionReq;
import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.TenantDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/tenant-deletions")
@RequiredArgsConstructor
public class TenantDeletionController {
    private final TenantDeletionService tenantDeletionRequestService;
    private final AuthzService authzService;
    @PostMapping
    @PreAuthorize("@authz.canRequestDeleteTenant(#req.tenantId())")
    public TenantDeletionRequestDTO create(@Valid @RequestBody CreateDeletionReq req,
                                           Authentication auth) {
        return tenantDeletionRequestService.create(req.tenantId(), req.reason(), auth);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.canApproveTicket(#id)")
    public ResponseEntity<TenantDeletionRequestDTO> approve(@PathVariable("id") UUID id,
                                            @Valid @RequestBody ApproveDeletionReq req,
                                            Authentication auth) {
        return ResponseEntity.ok(tenantDeletionRequestService.approve(id, req.note(), auth));
    }

    @GetMapping
    @PreAuthorize("@authz.canViewAllTenantDeletionRequests()")
    public ResponseEntity<List<TenantDeletionRequestDTO>> getAllTenantDeletionRequests(Authentication auth) {
        return ResponseEntity.ok(tenantDeletionRequestService.getAllTenantDeletionRequests());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewTenantDeletionRequest(#id)")
    public ResponseEntity<TenantDeletionRequestDTO> getTenantDeletionRequest(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(tenantDeletionRequestService.getTenantDeletionRequestDTO(id));
    }

    @GetMapping("/{id}/targets-status")
    @PreAuthorize("@authz.canViewTenantDeletionRequest(#id)")
    public ResponseEntity<Map<String, Object>> getTenantDeletionTargetsStatus(@PathVariable UUID id, Authentication auth) {
        var request = tenantDeletionRequestService.getTenantDeletionRequestDTO(id);
        return ResponseEntity.ok(tenantDeletionRequestService.getTenantDeletionTargetsStatus(request.tenantId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.canApproveTicket(#id)")
    public ResponseEntity<TenantDeletionRequestDTO> rejectTenantDeletion(@PathVariable UUID id, 
                                                                         @Valid @RequestBody ApproveDeletionReq req,
                                                                         Authentication auth) {
        return ResponseEntity.ok(tenantDeletionRequestService.rejectTenantDeletion(id, req.note(), auth));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.canApproveTicket(#id)")
    public ResponseEntity<TenantDeletionRequestDTO> completeTenantDeletion(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(tenantDeletionRequestService.completeTenantDeletion(id, auth));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<TenantDeletionRequestDTO>> getMyTenantDeletionRequests(Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        if (p.tenant() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tenantDeletionRequestService.getTenantDeletionRequestsByTenantId(p.tenant()));
    }

}
