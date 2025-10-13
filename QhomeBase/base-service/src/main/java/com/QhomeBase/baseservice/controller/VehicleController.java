package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.VehicleCreateDto;
import com.QhomeBase.baseservice.dto.VehicleDto;
import com.QhomeBase.baseservice.dto.VehicleUpdateDto;
import com.QhomeBase.baseservice.model.VehicleKind;
import com.QhomeBase.baseservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("@authz.canCreateVehicle(#dto.tenantId())")
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody VehicleCreateDto dto) {
        try {
            VehicleDto result = vehicleService.createVehicle(dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create")
    @PreAuthorize("@authz.canCreateVehicle(#dto.tenantId())")
    public ResponseEntity<VehicleDto> registerVehicle(@Valid @RequestBody VehicleCreateDto dto) {
        try {
            VehicleDto result = vehicleService.createVehicle(dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateVehicle(#id)")
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable UUID id, @Valid @RequestBody VehicleUpdateDto dto) {
        try {
            VehicleDto result = vehicleService.updateVehicle(dto, id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteVehicle(#id)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("@authz.canDeleteVehicle(#id)")
    public ResponseEntity<Void> hardDeleteVehicle(@PathVariable UUID id) {
        try {
            vehicleService.hardDeleteVehicle(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewVehicle(#id)")
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable UUID id) {
        try {
            VehicleDto result = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("@authz.canViewVehiclesByTenant(#tenantId)")
    public ResponseEntity<List<VehicleDto>> getVehiclesByTenantId(@PathVariable UUID tenantId) {
        List<VehicleDto> result = vehicleService.getVehiclesByTenantId(tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/resident/{residentId}")
    @PreAuthorize("@authz.canViewVehiclesByResident(#residentId)")
    public ResponseEntity<List<VehicleDto>> getVehiclesByResidentId(@PathVariable UUID residentId) {
        List<VehicleDto> result = vehicleService.getVehiclesByResidentId(residentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unit/{unitId}")
    @PreAuthorize("@authz.canViewVehiclesByUnit(#unitId)")
    public ResponseEntity<List<VehicleDto>> getVehiclesByUnitId(@PathVariable UUID unitId) {
        List<VehicleDto> result = vehicleService.getVehiclesByUnitId(unitId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tenant/{tenantId}/active")
    @PreAuthorize("@authz.canViewVehiclesByTenant(#tenantId)")
    public ResponseEntity<List<VehicleDto>> getActiveVehiclesByTenantId(@PathVariable UUID tenantId) {
        List<VehicleDto> result = vehicleService.getActiveVehiclesByTenantId(tenantId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.canManageVehicleStatus(#id)")
    public ResponseEntity<Void> changeVehicleStatus(@PathVariable UUID id, @RequestParam Boolean active) {
        try {
            vehicleService.changeVehicleStatus(id, active);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/kinds")
    public ResponseEntity<VehicleKind[]> getVehicleKinds() {
        return ResponseEntity.ok(VehicleKind.values());
    }
}
