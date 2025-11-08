package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.CreateMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.MaintenanceRequest;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.MaintenanceRequestRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;

    public MaintenanceRequestDto create(UUID userId, CreateMaintenanceRequestDto dto) {
        Unit unit = unitRepository.findById(dto.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));

        Household household = householdRepository.findCurrentHouseholdByUnitId(unit.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unit has no active household"));

        boolean belongsToUnit = isResidentInHousehold(resident, household);
        if (!belongsToUnit) {
            throw new IllegalArgumentException("You are not associated with this unit");
        }

        List<String> attachments = dto.attachments() != null
                ? new ArrayList<>(dto.attachments())
                : new ArrayList<>();

        if (attachments.size() > 3) {
            throw new IllegalArgumentException("Only up to 3 attachments are allowed");
        }

        String contactName = StringUtils.hasText(dto.contactName())
                ? dto.contactName().trim()
                : (resident.getFullName() != null ? resident.getFullName() : "Cư dân");
        String contactPhone = StringUtils.hasText(dto.contactPhone())
                ? dto.contactPhone().trim()
                : (resident.getPhone() != null ? resident.getPhone() : "");

        if (!StringUtils.hasText(contactPhone)) {
            throw new IllegalArgumentException("Contact phone is required");
        }

        MaintenanceRequest request = MaintenanceRequest.builder()
                .id(UUID.randomUUID())
                .unitId(unit.getId())
                .residentId(resident.getId())
                .createdBy(userId)
                .category(dto.category().trim())
                .title(dto.title().trim())
                .description(dto.description().trim())
                .attachments(attachments)
                .location(dto.location().trim())
                .preferredDatetime(dto.preferredDatetime())
                .contactName(contactName)
                .contactPhone(contactPhone)
                .note(dto.note())
                .status("PENDING")
                .build();

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        log.info("Created maintenance request {} for unit {}", saved.getId(), saved.getUnitId());
        return toDto(saved);
    }

    private boolean isResidentInHousehold(Resident resident, Household household) {
        if (resident == null || household == null) {
            return false;
        }
        if (household.getPrimaryResidentId() != null &&
                household.getPrimaryResidentId().equals(resident.getId())) {
            return true;
        }

        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(household.getId());
        return members.stream()
                .anyMatch(member -> resident.getId().equals(member.getResidentId()));
    }

    private MaintenanceRequestDto toDto(MaintenanceRequest entity) {
        return new MaintenanceRequestDto(
                entity.getId(),
                entity.getUnitId(),
                entity.getResidentId(),
                entity.getCreatedBy(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getAttachments(),
                entity.getLocation(),
                entity.getPreferredDatetime(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getNote(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

