package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.AddProgressNoteDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class MaintenanceRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_NEW = "NEW";
    
    private static final String RESPONSE_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String RESPONSE_STATUS_APPROVED = "APPROVED";
    private static final String RESPONSE_STATUS_REJECTED = "REJECTED";

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final LocalTime workingStart;
    private final LocalTime workingEnd;

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final NotificationClient notificationClient;

    public MaintenanceRequestService(
            MaintenanceRequestRepository maintenanceRequestRepository,
            UnitRepository unitRepository,
            ResidentRepository residentRepository,
            HouseholdRepository householdRepository,
            HouseholdMemberRepository householdMemberRepository,
            NotificationClient notificationClient,
            @Value("${maintenance.request.working.hours.start:08:00}") String workingStartStr,
            @Value("${maintenance.request.working.hours.end:18:00}") String workingEndStr) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.unitRepository = unitRepository;
        this.residentRepository = residentRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.notificationClient = notificationClient;
        this.workingStart = LocalTime.parse(workingStartStr, TIME_FORMATTER);
        this.workingEnd = LocalTime.parse(workingEndStr, TIME_FORMATTER);
    }

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

        ensureNoActiveRequest(resident.getId());

        OffsetDateTime normalizedPreferredDatetime = normalizePreferredDatetime(dto.preferredDatetime());
        validatePreferredDatetime(normalizedPreferredDatetime);

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
                .userId(userId)
                .category(dto.category().trim())
                .title(dto.title().trim())
                .description(dto.description().trim())
                .attachments(attachments)
                .location(dto.location().trim())
                .preferredDatetime(normalizedPreferredDatetime)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .note(dto.note())
                .status(STATUS_NEW)
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
                entity.getUserId(),
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
                entity.getUpdatedAt(),
                entity.getLastResentAt(),
                entity.isResendAlertSent(),
                entity.isCallAlertSent(),
                entity.getAdminResponse(),
                entity.getEstimatedCost(),
                entity.getRespondedBy(),
                entity.getRespondedAt(),
                entity.getResponseStatus(),
                entity.getProgressNotes()
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

    /**
     * Get paginated requests for a resident (all statuses)
     */
    public Map<String, Object> getMyRequestsPaged(UUID userId, int limit, int offset) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        List<MaintenanceRequest> requests = maintenanceRequestRepository
                .findByResidentIdWithPagination(resident.getId(), limit, offset);
        
        long total = maintenanceRequestRepository.countByResidentId(resident.getId());
        
        List<MaintenanceRequestDto> dtos = requests.stream()
                .map(this::toDto)
                .toList();
        
        return Map.of(
                "requests", dtos,
                "total", total,
                "limit", limit,
                "offset", offset
        );
    }

    public List<MaintenanceRequestDto> getPendingRequests() {
        List<MaintenanceRequest> requests = maintenanceRequestRepository
                .findByStatusOrderByCreatedAtAsc(STATUS_PENDING);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<MaintenanceRequestDto> getAllRequests() {
        List<MaintenanceRequest> requests = maintenanceRequestRepository.findAll();
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public MaintenanceRequestDto getRequestById(UUID requestId) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found with id: " + requestId));
        return toDto(request);
    }

    private void validatePreferredDatetime(OffsetDateTime preferredDatetime) {
        if (preferredDatetime == null) {
            throw new IllegalArgumentException("Preferred datetime is required");
        }

        OffsetDateTime now = OffsetDateTime.now(DEFAULT_TIMEZONE);
        if (preferredDatetime.isBefore(now)) {
            throw new IllegalArgumentException("Preferred datetime cannot be in the past");
        }

        LocalTime preferredTime = preferredDatetime.toLocalTime();
        if (preferredTime.isBefore(workingStart) || preferredTime.isAfter(workingEnd)) {
            throw new IllegalArgumentException(
                    String.format("Preferred time must be between %s and %s",
                            workingStart, workingEnd));
        }
    }

    private OffsetDateTime normalizePreferredDatetime(OffsetDateTime preferredDatetime) {
        if (preferredDatetime == null) {
            throw new IllegalArgumentException("Preferred datetime is required");
        }
        return preferredDatetime.atZoneSameInstant(DEFAULT_TIMEZONE).toOffsetDateTime();
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto respondToRequest(UUID adminId, UUID requestId, AdminMaintenanceResponseDto dto) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        // Allow responding to requests with status PENDING or any status for new workflow
        // Remove strict status check to allow more flexibility

        if (request.getResponseStatus() != null && RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            throw new IllegalStateException("Request already has a pending response awaiting approval");
        }

        request.setAdminResponse(dto.adminResponse());
        request.setEstimatedCost(dto.estimatedCost());
        request.setRespondedBy(adminId);
        request.setRespondedAt(OffsetDateTime.now());
        request.setResponseStatus(RESPONSE_STATUS_PENDING_APPROVAL);
        // Set status to PENDING when accepting/responding
        request.setStatus(STATUS_PENDING);
        
        if (dto.note() != null && !dto.note().isBlank()) {
            request.setNote(dto.note().trim());
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceResponseReceived(saved);
        log.info("Admin {} responded to maintenance request {} and set status to PENDING", adminId, requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto approveRequest(UUID adminId, UUID requestId, AdminServiceRequestActionDto dto) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        // For deny action: set status to CANCELLED
        // This method is used for denying requests
        request.setStatus(STATUS_CANCELLED);
        
        if (dto != null && dto.note() != null && !dto.note().isBlank()) {
            request.setNote(dto.note().trim());
        }
        
        if (request.getResponseStatus() == null || RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            request.setResponseStatus(RESPONSE_STATUS_REJECTED);
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceCancelled(saved);
        log.info("Admin {} denied maintenance request {} and set status to CANCELLED", adminId, requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto completeRequest(UUID staffId, UUID requestId, AdminServiceRequestActionDto dto) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only in-progress requests can be marked done");
        }

        request.setStatus(STATUS_DONE);
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceCompleted(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto approveResponse(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only approve responses for your own requests");
        }

        if (!RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            throw new IllegalStateException("No pending response to approve");
        }

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Request status must be PENDING to approve response");
        }

        request.setResponseStatus(RESPONSE_STATUS_APPROVED);
        request.setStatus(STATUS_IN_PROGRESS);
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceResponseApproved(saved);
        log.info("Resident {} approved response for maintenance request {}", resident.getId(), requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto rejectResponse(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only reject responses for your own requests");
        }

        if (!RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            throw new IllegalStateException("No pending response to reject");
        }

        request.setResponseStatus(RESPONSE_STATUS_REJECTED);
        request.setStatus(STATUS_CANCELLED);
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceResponseRejected(saved);
        log.info("Resident {} rejected response for maintenance request {}", resident.getId(), requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto cancelRequest(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (STATUS_DONE.equalsIgnoreCase(request.getStatus()) ||
                STATUS_CANCELLED.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Cannot cancel a completed or already cancelled request");
        }

        request.setStatus(STATUS_CANCELLED);
        if (request.getResponseStatus() == null || RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            request.setResponseStatus(RESPONSE_STATUS_REJECTED);
        }
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        // Không gửi notification khi cư dân tự hủy request
        // notifyMaintenanceCancelled(saved);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto resendRequest(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only resend your own requests");
        }

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be resent");
        }

        OffsetDateTime now = OffsetDateTime.now();
        request.setLastResentAt(now);
        request.setResendAlertSent(false); // Reset để có thể gửi reminder lại nếu cần
        request.setCallAlertSent(false); // Reset call alert nếu đã set
        
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        log.info("Resent maintenance request {} at {}", saved.getId(), now);
        return toDto(saved);
    }

    private void ensureNoActiveRequest(UUID residentId) {
        if (residentId == null) {
            return;
        }
        boolean hasPending = maintenanceRequestRepository
                .existsByResidentIdAndStatusIgnoreCase(residentId, STATUS_PENDING);
        boolean hasInProgress = maintenanceRequestRepository
                .existsByResidentIdAndStatusIgnoreCase(residentId, STATUS_IN_PROGRESS);
        if (hasPending || hasInProgress) {
            throw new IllegalStateException(
                    "Bạn đang có yêu cầu sửa chữa chưa hoàn tất. Vui lòng chờ đơn hiện tại sang trạng thái DONE trước khi tạo thêm.");
        }
    }

    private void notifyMaintenanceInProgress(MaintenanceRequest request, String note) {
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\" đang được xử lý.");
        if (note != null && !note.isBlank()) {
            body.append(' ').append(note.trim());
        } else if (request.getPreferredDatetime() != null) {
            body.append(" Kỹ thuật viên sẽ liên hệ cho lịch hẹn dự kiến vào ")
                    .append(request.getPreferredDatetime());
        }

        sendMaintenanceNotification(
                request,
                "Yêu cầu sửa chữa đang xử lý",
                body.toString(),
                STATUS_IN_PROGRESS
        );
    }

    private void notifyMaintenanceCompleted(MaintenanceRequest request, String note) {
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\" đã hoàn tất.");
        if (note != null && !note.isBlank()) {
            body.append(' ').append(note.trim());
        }

        sendMaintenanceNotification(
                request,
                "Yêu cầu sửa chữa đã hoàn tất",
                body.toString(),
                STATUS_DONE
        );
    }

    private void notifyMaintenanceResponseReceived(MaintenanceRequest request) {
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\" đã nhận được phản hồi từ admin.");
        
        if (request.getEstimatedCost() != null) {
            body.append(" Chi phí ước tính: ")
                    .append(String.format("%,.0f", request.getEstimatedCost()))
                    .append(" VNĐ.");
        }
        body.append(" Vui lòng xem chi tiết và xác nhận.");

        sendMaintenanceNotification(
                request,
                "Phản hồi từ admin về yêu cầu sửa chữa",
                body.toString(),
                request.getStatus()
        );
    }

    private void notifyMaintenanceResponseApproved(MaintenanceRequest request) {
        StringBuilder body = new StringBuilder("Bạn đã xác nhận phản hồi từ admin cho yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\". Yêu cầu đang được xử lý.");

        sendMaintenanceNotification(
                request,
                "Đã xác nhận phản hồi từ admin",
                body.toString(),
                STATUS_IN_PROGRESS
        );
    }

    private void notifyMaintenanceResponseRejected(MaintenanceRequest request) {
        StringBuilder body = new StringBuilder("Bạn đã từ chối phản hồi từ admin cho yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\". Yêu cầu đã được hủy.");

        sendMaintenanceNotification(
                request,
                "Đã từ chối phản hồi từ admin",
                body.toString(),
                STATUS_CANCELLED
        );
    }

    private void notifyMaintenanceCancelled(MaintenanceRequest request) {
        StringBuilder body = new StringBuilder("Yêu cầu sửa chữa \"")
                .append(request.getTitle())
                .append("\" đã được hủy.");

        sendMaintenanceNotification(
                request,
                "Yêu cầu sửa chữa đã được hủy",
                body.toString(),
                STATUS_CANCELLED
        );
    }

    private void sendMaintenanceNotification(
            MaintenanceRequest request,
            String title,
            String body,
            String status
    ) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [MaintenanceRequest] Missing residentId for request {}", request.getId());
            return;
        }

        Unit unit = null;
        UUID unitId = request.getUnitId();
        if (unitId != null) {
            unit = unitRepository.findById(unitId).orElse(null);
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", status);
        data.put("category", request.getCategory());

        UUID buildingId = (unit != null && unit.getBuilding() != null)
                ? unit.getBuilding().getId()
                : null;

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                title,
                body,
                request.getId(),
                "MAINTENANCE_REQUEST",
                data
        );
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto addProgressNote(UUID staffId, UUID requestId, String note) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Progress notes can only be added when request status is IN_PROGRESS");
        }

        // Append new note to existing progress notes
        String existingNotes = request.getProgressNotes();
        String newNote = note != null ? note.trim() : "";
        
        if (newNote.isEmpty()) {
            throw new IllegalArgumentException("Progress note cannot be empty");
        }

        // Format: append with timestamp and separator
        OffsetDateTime now = OffsetDateTime.now();
        String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String formattedNote = String.format("[%s] %s", timestamp, newNote);
        
        if (existingNotes == null || existingNotes.isBlank()) {
            request.setProgressNotes(formattedNote);
        } else {
            request.setProgressNotes(existingNotes + "\n\n" + formattedNote);
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        log.info("Staff {} added progress note to maintenance request {}", staffId, requestId);
        return toDto(saved);
    }
}

