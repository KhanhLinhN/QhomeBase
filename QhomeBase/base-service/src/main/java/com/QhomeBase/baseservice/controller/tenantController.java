package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.tenantRequestDto;
import com.QhomeBase.baseservice.dto.tenantResponseDto;
import com.QhomeBase.baseservice.dto.tenantUpdateDto;
import com.QhomeBase.baseservice.repository.tenantRepository;
import com.QhomeBase.baseservice.service.tenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class tenantController {
    private final tenantService tenantService;
    public tenantController(tenantService tenantService) {
        this.tenantService = tenantService;
    }
    @PostMapping
    public ResponseEntity<tenantResponseDto> createTenants(@RequestBody  tenantRequestDto dto) {
        return ResponseEntity.ok(tenantService.createTenants(dto));
    }
    @PutMapping("/{id}")
    public ResponseEntity<tenantResponseDto> updateTenants(@RequestBody tenantUpdateDto dto, @RequestParam  UUID tenantId) {
        return ResponseEntity.ok(tenantService.updateTenant(dto, tenantId));
    }
}
