package com.QhomeBase.baseservice.controller;


import com.QhomeBase.baseservice.dto.ApproveDeletionReq;
import com.QhomeBase.baseservice.dto.CreateDeletionReq;
import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.service.TenantDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RestController
@RequestMapping("api/tenant-deletions")
@RequiredArgsConstructor
public class TenantDeletionController {
    private final TenantDeletionService tenantDeletionRequestService;
    private final AuthzService authzService;
    @PostMapping
    @PreAuthorize("@authz.canManageTenant(#req.tenantId())")
    public TenantDeletionRequestDTO create(@Valid @RequestBody CreateDeletionReq req,
                                           Authentication auth) {
        return tenantDeletionRequestService.create(req.tenantId(), req.reason(), auth);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.canApproveTicket(#id)")
    public TenantDeletionRequestDTO approve(@PathVariable("id") UUID id,
                                            @Valid @RequestBody ApproveDeletionReq req,
                                            Authentication auth) {
        var e = tenantDeletionRequestService.getTenantDeletionRequestDTO(id);
        return tenantDeletionRequestService.approve(id, req.note(), auth);
    }



}
