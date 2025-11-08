package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceDto;
import com.QhomeBase.assetmaintenanceservice.service.ServiceConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceConfigService serviceConfigService;

    @PostMapping
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceDto> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceDto created = serviceConfigService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable UUID id) {
        ServiceDto service = serviceConfigService.findById(id);
        return ResponseEntity.ok(service);
    }
}

