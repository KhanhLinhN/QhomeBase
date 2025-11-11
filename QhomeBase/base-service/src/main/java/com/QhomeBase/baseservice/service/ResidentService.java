package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ResidentDto;
import com.QhomeBase.baseservice.dto.StaffResidentSyncRequest;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.ResidentStatus;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResidentService {

    private final ResidentRepository residentRepository;

    public List<ResidentDto> findAll() {
        return residentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ResidentDto> findAllByStatus(ResidentStatus status) {
        if (status == null) {
            return findAll();
        }
        return residentRepository.findAllByStatus(status).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ResidentDto> search(String term) {
        if (!StringUtils.hasText(term)) {
            return Collections.emptyList();
        }
        return residentRepository.searchByTerm(term.trim()).stream()
                .map(this::toDto)
                .toList();
    }

    public ResidentDto getById(UUID residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + residentId));
        return toDto(resident);
    }

    public Optional<Resident> findEntityById(UUID residentId) {
        return residentRepository.findById(residentId);
    }

    public Optional<ResidentDto> findByUserId(UUID userId) {
        return residentRepository.findByUserId(userId)
                .map(this::toDto);
    }

    public Optional<Resident> findEntityByUserId(UUID userId) {
        return residentRepository.findByUserId(userId);
    }

    public ResidentDto getByUserId(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        return toDto(resident);
    }

    public boolean existsEmail(String email, UUID excludeId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByEmail(email)
                : residentRepository.existsByEmailAndIdNot(email, excludeId);
    }

    public boolean existsPhone(String phone, UUID excludeId) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByPhone(phone)
                : residentRepository.existsByPhoneAndIdNot(phone, excludeId);
    }

    public boolean existsNationalId(String nationalId, UUID excludeId) {
        if (!StringUtils.hasText(nationalId)) {
            return false;
        }
        return excludeId == null
                ? residentRepository.existsByNationalId(nationalId)
                : residentRepository.existsByNationalIdAndIdNot(nationalId, excludeId);
    }

    public ResidentDto toDto(Resident resident) {
        if (resident == null) {
            return null;
        }
        return new ResidentDto(
                resident.getId(),
                resident.getFullName(),
                resident.getPhone(),
                resident.getEmail(),
                resident.getNationalId(),
                resident.getDob(),
                resident.getStatus(),
                resident.getUserId(),
                resident.getCreatedAt(),
                resident.getUpdatedAt()
        );
    }

    @Transactional
    public ResidentDto updateStatus(UUID residentId, ResidentStatus status) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found: " + residentId));
        if (status == null) {
            throw new IllegalArgumentException("Resident status must not be null");
        }
        resident.setStatus(status);
        Resident saved = residentRepository.save(resident);
        return toDto(saved);
    }

    @Transactional
    public ResidentDto syncStaffResident(StaffResidentSyncRequest request) {
        if (request.userId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Resident resident = residentRepository.findByUserId(request.userId())
                .orElseGet(() -> Resident.builder()
                        .fullName(resolveFullName(request.fullName(), request.email()))
                        .email(request.email())
                        .phone(request.phone())
                        .status(ResidentStatus.ACTIVE)
                        .userId(request.userId())
                        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build());

        resident.setFullName(resolveFullName(request.fullName(), request.email()));
        resident.setEmail(request.email());
        resident.setPhone(request.phone());
        resident.setStatus(ResidentStatus.ACTIVE);
        resident.setUserId(request.userId());
        resident.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Resident saved = residentRepository.save(resident);
        return toDto(saved);
    }

    private String resolveFullName(String fullName, String fallbackEmail) {
        if (StringUtils.hasText(fullName)) {
            return fullName.trim();
        }
        if (StringUtils.hasText(fallbackEmail)) {
            return fallbackEmail;
        }
        return "Staff";
    }
}