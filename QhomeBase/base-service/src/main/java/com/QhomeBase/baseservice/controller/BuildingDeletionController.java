package com.QhomeBase.baseservice.controller;


import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.service.BuildingDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/delete-buildings")
@RequiredArgsConstructor
public class BuildingDeletionController {
    private final BuildingDeletionService service;
    private final AuthzService authzService;
    @PostMapping
    @PreAuthorize("@authz.canRequestDeleteBuilding(#dto.tenantId())")
     public BuildingDeletionRequestDto create(@Valid @RequestBody BuildingDeletionCreateReq dto, Authentication auth) {
        return service.create(dto,auth);
    }
    @PostMapping("/approve")
    public BuildingDeletionRequestDto approve(@RequestParam UUID requestId, @Valid @RequestBody BuildingDeletionApproveReq dto, Authentication auth) {
        // Lấy tenantId từ request để check authorization
        var request = service.getById(requestId);
        if (!authzService.canApproveDeleteBuilding(request.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return service.approve(requestId,dto,auth);
    }
    @PostMapping("/{id}/reject")
    public BuildingDeletionRequestDto reject(@PathVariable("id") UUID id,
                                            @Valid @RequestBody BuildingDeletionRejectReq req,
                                            Authentication auth) {
        // Lấy tenantId từ request để check authorization
        var request = service.getById(id);
        if (!authzService.canRejectDeleteBuilding(request.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return service.reject(id, req, auth);
    }

    @GetMapping("/{id}")
    public BuildingDeletionRequestDto getById(@PathVariable("id") UUID id) {
        var request = service.getById(id);
        if (!authzService.canViewDeleteBuilding(id)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return request;
    }

    @GetMapping("/tenant/{tenantId}")
    public List<BuildingDeletionRequestDto> getByTenantId(@PathVariable("tenantId") UUID tenantId) {
        if (!authzService.canViewDeleteBuilding(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return service.getByTenantId(tenantId);
    }

    @GetMapping("/building/{buildingId}")
    public List<BuildingDeletionRequestDto> getByBuildingId(@PathVariable("buildingId") UUID buildingId) {
        if (!authzService.canViewDeleteBuilding(buildingId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return service.getByBuildingId(buildingId);
    }

    @GetMapping("/pending")
    public List<BuildingDeletionRequestDto> getPendingRequests() {
        if (!authzService.canViewAllDeleteBuildings()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return service.getPendingRequests();
    }
}
