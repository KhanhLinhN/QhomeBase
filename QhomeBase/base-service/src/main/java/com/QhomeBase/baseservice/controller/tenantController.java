package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.TenantRequestDto;
import com.QhomeBase.baseservice.dto.TenantResponseDto;
import com.QhomeBase.baseservice.dto.TenantUpdateDto;
import com.QhomeBase.baseservice.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class tenantController {
    private final TenantService tenantService;
    public tenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    @PostMapping
    public ResponseEntity<TenantResponseDto> createTenants(@Valid @RequestBody TenantRequestDto dto, Authentication authentication) {
        return ResponseEntity.ok(tenantService.createTenants(dto, authentication));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDto> getTenant(@PathVariable UUID id) {
        var tenant = tenantService.findTenant(id);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tenant);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponseDto>> getAllTenants(Authentication authentication) {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }




    @PutMapping("/{id}")
    public ResponseEntity<TenantResponseDto> updateTenant(@PathVariable UUID id, @Valid @RequestBody TenantUpdateDto dto, Authentication authentication) {
        return ResponseEntity.ok(tenantService.updateTenant(dto, id, authentication));
    }

}
