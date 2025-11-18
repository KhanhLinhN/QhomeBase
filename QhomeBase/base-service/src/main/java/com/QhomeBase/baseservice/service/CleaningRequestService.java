package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.CleaningRequestDto;
import com.QhomeBase.baseservice.dto.CreateCleaningRequestDto;
import com.QhomeBase.baseservice.model.CleaningRequest;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.CleaningRequestRepository;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CleaningRequestService {

    private static final Map<String, BigDecimal> DEFAULT_DURATIONS = Map.of(
            "Dọn dẹp cơ bản", BigDecimal.valueOf(1),
            "Dọn dẹp tổng thể", BigDecimal.valueOf(2),
            "Dọn bếp", BigDecimal.valueOf(1.5),
            "Dọn phòng khách", BigDecimal.valueOf(1.5),
            "Giặt rèm", BigDecimal.valueOf(2),
            "Khử mùi toàn căn", BigDecimal.valueOf(1.5),
            "Vệ sinh thiết bị", BigDecimal.valueOf(1)
    );

    private final CleaningRequestRepository cleaningRequestRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final NotificationClient notificationClient;

    @SuppressWarnings("null")
    public CleaningRequestDto create(UUID userId, CreateCleaningRequestDto dto) {
        Unit unit = unitRepository.findById(dto.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));

        Household household = householdRepository.findCurrentHouseholdByUnitId(unit.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unit has no active household"));

        if (!isResidentInHousehold(resident, household)) {
            throw new IllegalArgumentException("You are not associated with this unit");
        }

        String normalizedType = dto.cleaningType().trim();
        BigDecimal duration = DEFAULT_DURATIONS.getOrDefault(
                normalizedType,
                dto.durationHours().setScale(2, RoundingMode.HALF_UP)
        ).setScale(2, RoundingMode.HALF_UP);
        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }

        String contactPhone = StringUtils.hasText(dto.contactPhone())
                ? dto.contactPhone().trim()
                : (resident.getPhone() != null ? resident.getPhone() : "");

        if (!StringUtils.hasText(contactPhone)) {
            throw new IllegalArgumentException("Contact phone is required");
        }

        CleaningRequest request = CleaningRequest.builder()
                .id(UUID.randomUUID())
                .unitId(unit.getId())
                .residentId(resident.getId())
                .createdBy(userId)
                .cleaningType(normalizedType)
                .cleaningDate(dto.cleaningDate())
                .startTime(dto.startTime())
                .durationHours(duration)
                .location(dto.location().trim())
                .note(StringUtils.hasText(dto.note()) ? dto.note().trim() : null)
                .contactPhone(contactPhone)
                .extraServices(dto.extraServices())
                .paymentMethod(StringUtils.hasText(dto.paymentMethod()) ? dto.paymentMethod().trim() : null)
                .status("PENDING")
                .build();

        CleaningRequest saved = cleaningRequestRepository.save(request);
        log.info("Created cleaning request {} for unit {}", saved.getId(), saved.getUnitId());
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public List<CleaningRequestDto> getMyRequests(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByResidentIdOrderByCreatedAtDesc(resident.getId());
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CleaningRequestDto> getPendingRequests() {
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByStatusOrderByCreatedAtAsc("PENDING");
        return requests.stream()
                .map(this::toDto)
                .toList();
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

    private CleaningRequestDto toDto(CleaningRequest entity) {
        return new CleaningRequestDto(
                entity.getId(),
                entity.getUnitId(),
                entity.getResidentId(),
                entity.getCreatedBy(),
                entity.getCleaningType(),
                entity.getCleaningDate(),
                entity.getStartTime(),
                entity.getDurationHours(),
                entity.getLocation(),
                entity.getNote(),
                entity.getContactPhone(),
                entity.getExtraServices(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @SuppressWarnings("null")
    public CleaningRequestDto approveRequest(UUID adminId, UUID requestId, AdminServiceRequestActionDto dto) {
        CleaningRequest request = cleaningRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }

        request.setStatus("APPROVED");
        CleaningRequest saved = cleaningRequestRepository.save(request);
        notifyCleaningApproval(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    private void notifyCleaningApproval(CleaningRequest request, String note) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [CleaningRequest] Missing residentId for request {}", request.getId());
            return;
        }

        Unit unit = null;
        UUID unitId = request.getUnitId();
        if (unitId != null) {
            unit = unitRepository.findById(unitId).orElse(null);
        }

        String message = "Yêu cầu dọn dẹp \"" + request.getCleaningType() + "\" đã được duyệt.";
        if (note != null && !note.isBlank()) {
            message += " " + note.trim();
        } else {
            message += " Nhân viên sẽ liên hệ để sắp xếp lịch phù hợp.";
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "CLEANING_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", request.getStatus());
        data.put("location", request.getLocation());

        UUID buildingId = (unit != null && unit.getBuilding() != null)
                ? unit.getBuilding().getId()
                : null;

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                "Yêu cầu dọn dẹp đã được duyệt",
                message,
                request.getId(),
                "CLEANING_REQUEST",
                data
        );
    }
}

