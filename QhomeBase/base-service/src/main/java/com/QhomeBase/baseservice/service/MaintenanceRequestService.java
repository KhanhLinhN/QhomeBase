package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaintenanceRequestService {

    private static final LocalTime WORKING_START = LocalTime.of(8, 0);
    private static final LocalTime WORKING_END = LocalTime.of(18, 0);

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final NotificationClient notificationClient;

    @SuppressWarnings("null")
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

        validatePreferredDatetime(dto.preferredDatetime());

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

    @SuppressWarnings("null")
    public List<MaintenanceRequestDto> getMyRequests(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        List<MaintenanceRequest> requests = maintenanceRequestRepository
                .findByResidentIdOrderByCreatedAtDesc(resident.getId());
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<MaintenanceRequestDto> getPendingRequests() {
        List<MaintenanceRequest> requests = maintenanceRequestRepository
                .findByStatusOrderByCreatedAtAsc("PENDING");
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    private void validatePreferredDatetime(OffsetDateTime preferredDatetime) {
        if (preferredDatetime == null) {
            throw new IllegalArgumentException("Preferred datetime is required");
        }

        OffsetDateTime now = OffsetDateTime.now(preferredDatetime.getOffset());
        if (preferredDatetime.isBefore(now)) {
            throw new IllegalArgumentException("Preferred datetime cannot be in the past");
        }

        LocalTime preferredTime = preferredDatetime.toLocalTime();
        if (preferredTime.isBefore(WORKING_START) || preferredTime.isAfter(WORKING_END)) {
            throw new IllegalArgumentException(
                    String.format("Preferred time must be between %s and %s",
                            WORKING_START, WORKING_END));
        }
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto approveRequest(UUID adminId, UUID requestId, AdminServiceRequestActionDto dto) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        request.setStatus("APPROVED");
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceApproval(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    private void notifyMaintenanceApproval(MaintenanceRequest request, String note) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [MaintenanceRequest] Missing residentId for request {}", request.getId());
            return;
        }

        Unit unit = null;
        UUID unitId = request.getUnitId();
        if (unitId != null) {
            unit = unitRepository.findById(unitId).orElse(null);
        }

        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\" đã được duyệt.");
        if (note != null && !note.isBlank()) {
            body.append(' ').append(note.trim());
        } else if (request.getPreferredDatetime() != null) {
            body.append(" Kỹ thuật viên sẽ ghé vào ")
                    .append(request.getPreferredDatetime());
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", request.getStatus());
        data.put("category", request.getCategory());

        UUID buildingId = (unit != null && unit.getBuilding() != null)
                ? unit.getBuilding().getId()
                : null;

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                "Yêu cầu sửa chữa đã được duyệt",
                body.toString(),
                request.getId(),
                "MAINTENANCE_REQUEST",
                data
        );
    }
}

