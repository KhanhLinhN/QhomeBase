package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.VehicleRegistrationApproveDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationCreateDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationRejectDto;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import com.QhomeBase.baseservice.service.VehicleRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicle-registrations")
@RequiredArgsConstructor
public class VehicleRegistrationController {
    private final VehicleRegistrationService vehicleRegistrationService;

    @PostMapping
    @PreAuthorize("@authz.canCreateVehicleRegistration(#dto.tenantId())")
    public ResponseEntity<VehicleRegistrationDto> createRegistrationRequest(
            @Valid @RequestBody VehicleRegistrationCreateDto dto, 
            Authentication auth) {
        try {
            VehicleRegistrationDto result = vehicleRegistrationService.createRegistrationRequest(dto, auth);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.canApproveVehicleRegistration(#id)")
    public ResponseEntity<VehicleRegistrationDto> approveRequest(
            @PathVariable UUID id, 
            @Valid @RequestBody VehicleRegistrationApproveDto dto, 
            Authentication auth) {
        try {
            VehicleRegistrationDto result = vehicleRegistrationService.approveRequest(id, dto, auth);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.canApproveVehicleRegistration(#id)")
    public ResponseEntity<VehicleRegistrationDto> rejectRequest(
            @PathVariable UUID id, 
            @Valid @RequestBody VehicleRegistrationRejectDto dto, 
            Authentication auth) {
        try {
            VehicleRegistrationDto result = vehicleRegistrationService.rejectRequest(id, dto, auth);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authz.canCancelVehicleRegistration(#id)")
    public ResponseEntity<VehicleRegistrationDto> cancelRequest(
            @PathVariable UUID id, 
            Authentication auth) {
        try {
            VehicleRegistrationDto result = vehicleRegistrationService.cancelRequest(id, auth);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewVehicleRegistration(#id)")
    public ResponseEntity<VehicleRegistrationDto> getRequestById(@PathVariable UUID id) {
        try {
            VehicleRegistrationDto result = vehicleRegistrationService.getRequestById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("@authz.canViewVehicleRegistrationsByTenant(#tenantId)")
    public ResponseEntity<List<VehicleRegistrationDto>> getRequestsByTenantId(@PathVariable UUID tenantId) {
        List<VehicleRegistrationDto> result = vehicleRegistrationService.getRequestsByTenantId(tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("@authz.canViewAllVehicleRegistrations()")
    public ResponseEntity<List<VehicleRegistrationDto>> getRequestsByStatus(@PathVariable VehicleRegistrationStatus status) {
        List<VehicleRegistrationDto> result = vehicleRegistrationService.getRequestsByStatus(status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending")
    @PreAuthorize("@authz.canViewAllVehicleRegistrations()")
    public ResponseEntity<List<VehicleRegistrationDto>> getPendingRequests() {
        List<VehicleRegistrationDto> result = vehicleRegistrationService.getPendingRequests();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("@authz.canViewVehicleRegistrations()")
    public ResponseEntity<List<VehicleRegistrationDto>> getRequestsByVehicleId(@PathVariable UUID vehicleId) {
        List<VehicleRegistrationDto> result = vehicleRegistrationService.getRequestsByVehicleId(vehicleId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statuses")
    public ResponseEntity<VehicleRegistrationStatus[]> getRegistrationStatuses() {
        return ResponseEntity.ok(VehicleRegistrationStatus.values());
    }
}
