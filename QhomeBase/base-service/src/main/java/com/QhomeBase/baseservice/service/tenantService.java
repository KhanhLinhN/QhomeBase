package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.tenantRequestDto;
import com.QhomeBase.baseservice.dto.tenantResponseDto;
import com.QhomeBase.baseservice.dto.tenantUpdateDto;
import com.QhomeBase.baseservice.model.tenant;
import com.QhomeBase.baseservice.repository.tenantRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class tenantService {
    private final tenantRepository tenantRepository;
    public tenantService(tenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    public tenantResponseDto createTenants(tenantRequestDto dto) {
        if (tenantRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("tenantCode already exists");
        }
        tenant entity = tenant.builder()
                .code(dto.getCode()).name(dto.getName())
                .contact(dto.getContact())
                .email(dto.getEmail())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .description(dto.getDescription())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
        tenant save =  tenantRepository.save(entity);
        return mapToResponse(save);
    }
    public tenantResponseDto mapToResponse(tenant t) {
        return new tenantResponseDto(
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getContact(),
                t.getEmail(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getCreatedBy(),
                t.getUpdatedBy(),
                t.isDeleted()

        );
    }
    public tenantResponseDto findTenant (UUID id) {
        tenant t = tenantRepository.findById(id).orElse(null);
        return mapToResponse(t);
    }
    public tenantResponseDto updateTenant (tenantUpdateDto dto, UUID id) {
        tenant t = tenantRepository.findById(id).orElse(null);
        t.setName((dto.getName()));
        t.setContact((dto.getContact()));
        t.setEmail(dto.getEmail());
        t.setStatus(dto.getStatus() != null ? dto.getStatus() : t.getStatus());
        t.setDescription((dto.getDescription()));
        t.setUpdatedAt(Instant.now());
        t.setUpdatedBy("system");
        tenant save =   tenantRepository.save(t);
        return mapToResponse(save);
    }

}
