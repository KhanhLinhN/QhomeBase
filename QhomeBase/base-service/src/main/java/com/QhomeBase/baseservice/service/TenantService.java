package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantRequestDto;
import com.QhomeBase.baseservice.dto.TenantResponseDto;
import com.QhomeBase.baseservice.dto.TenantUpdateDto;
import com.QhomeBase.baseservice.model.Tenant;
import com.QhomeBase.baseservice.repository.TenantRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;


import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class TenantService {
    private final TenantRepository tenantRepository;
    private final Validator validator;

    public TenantService(TenantRepository tenantRepository, Validator validator) {
        this.tenantRepository = tenantRepository;
        this.validator = validator;
    }
    public TenantResponseDto createTenants(@Valid TenantRequestDto dto, Authentication authentication) {

        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);


        if (tenantRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("tenantCode already exists");
        }

        var u = (UserPrincipal) authentication.getPrincipal();
        var createdId = u.uid().toString();

        Tenant entity = Tenant.builder()
                .code(dto.getCode()).name(dto.getName())
                .contact(dto.getContact())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .description(dto.getDescription())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(createdId)
                .updatedBy(createdId)
                .build();
        Tenant save =  tenantRepository.save(entity);

        System.out.println("=== TENANT CREATED ===");
        System.out.println("Tenant ID: " + save.getId());
        System.out.println("Tenant Code: " + save.getCode());
        System.out.println("Tenant Name: " + save.getName());
        System.out.println("======================");
        
        return mapToResponse(save);
    }
    public TenantResponseDto mapToResponse(Tenant t) {
        return new TenantResponseDto(
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getContact(),
                t.getEmail(),
                t.getAddress(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getCreatedBy(),
                t.getUpdatedBy(),
                t.isDeleted()

        );
    }
    public TenantResponseDto findTenant (UUID id) {

        Tenant t = tenantRepository.findById(id).orElse(null);

        
        return mapToResponse(t);
    }
    public TenantResponseDto updateTenant (TenantUpdateDto dto, UUID id,  Authentication authentication) {
        Tenant t = tenantRepository.findById(id).orElse(null);
        if (t == null) {
            throw new IllegalArgumentException("Tenant not found with ID: " + id);
        }
        var u = (UserPrincipal) authentication.getPrincipal();
        var createdId = u.uid().toString();
        t.setName((dto.getName()));
        t.setContact((dto.getContact()));
        t.setEmail(dto.getEmail());
        t.setAddress(dto.getAddress());
        t.setStatus(dto.getStatus() != null ? dto.getStatus() : t.getStatus());
        t.setDescription((dto.getDescription()));
        t.setUpdatedAt(Instant.now());
        t.setUpdatedBy(createdId);
        Tenant save =   tenantRepository.save(t);
        return mapToResponse(save);
    }

    public List<TenantResponseDto> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        return tenants.stream()
                .filter(tenant -> !tenant.isDeleted()) // Only return non-deleted tenants
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}
