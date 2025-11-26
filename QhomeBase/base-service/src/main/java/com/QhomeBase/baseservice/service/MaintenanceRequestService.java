package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
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
import com.QhomeBase.baseservice.service.IamClientService;
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
    private final TrelloService trelloService;
    private final IamClientService iamClientService;
    
    private final String todoListId;
    private final String inProgressListId;
    private final String doneListId;

    public MaintenanceRequestService(
            MaintenanceRequestRepository maintenanceRequestRepository,
            UnitRepository unitRepository,
            ResidentRepository residentRepository,
            HouseholdRepository householdRepository,
            HouseholdMemberRepository householdMemberRepository,
            NotificationClient notificationClient,
            TrelloService trelloService,
            IamClientService iamClientService,
            @Value("${maintenance.request.working.hours.start:08:00}") String workingStartStr,
            @Value("${maintenance.request.working.hours.end:18:00}") String workingEndStr,
            @Value("${trello.list.id.todo:}") String todoListId,
            @Value("${trello.list.id.inprogress:}") String inProgressListId,
            @Value("${trello.list.id.done:}") String doneListId) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.unitRepository = unitRepository;
        this.residentRepository = residentRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.notificationClient = notificationClient;
        this.trelloService = trelloService;
        this.iamClientService = iamClientService;
        this.workingStart = LocalTime.parse(workingStartStr, TIME_FORMATTER);
        this.workingEnd = LocalTime.parse(workingEndStr, TIME_FORMATTER);
        this.todoListId = todoListId;
        this.inProgressListId = inProgressListId;
        this.doneListId = doneListId;
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
                entity.getTrelloCardId(),
                entity.getAssignedStaffId()
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
        
        // Save staff ID if provided
        if (dto.staffId() != null) {
            request.setAssignedStaffId(dto.staffId());
        }
        
        if (dto.note() != null && !dto.note().isBlank()) {
            request.setNote(dto.note().trim());
        }

        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        
        // Create Trello card if not exists and assign to staff if staff ID is provided
        if (saved.getTrelloCardId() == null || saved.getTrelloCardId().isEmpty()) {
            String cardTitle = String.format("[MR-%s] %s", saved.getId().toString().substring(0, 8), saved.getTitle());
            String cardDescription = buildTrelloCardDescription(saved);
            String cardId = trelloService.createCard(saved.getId(), cardTitle, cardDescription, null);
            if (cardId != null) {
                saved.setTrelloCardId(cardId);
                saved = maintenanceRequestRepository.save(saved);
            }
        }
        
        // Assign Trello card to staff if staff ID is provided
        if (dto.staffId() != null && saved.getTrelloCardId() != null && !saved.getTrelloCardId().isEmpty()) {
            assignTrelloCardToStaff(saved.getTrelloCardId(), dto.staffId());
        }
        
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
        
        // Move Trello card to Done list if exists
        if (request.getTrelloCardId() != null && !request.getTrelloCardId().isEmpty()) {
            trelloService.moveCardToList(request.getTrelloCardId(), trelloService.getDoneListId());
        }
        
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceCompleted(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public MaintenanceRequestDto updateStatus(UUID adminId, UUID requestId, String status) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance request not found"));

        String normalizedStatus = status.toUpperCase();
        
        // Validate status
        if (!normalizedStatus.equals(STATUS_NEW) && 
            !normalizedStatus.equals(STATUS_PENDING) && 
            !normalizedStatus.equals(STATUS_IN_PROGRESS) && 
            !normalizedStatus.equals(STATUS_DONE) && 
            !normalizedStatus.equals(STATUS_CANCELLED)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        String oldStatus = request.getStatus();
        request.setStatus(normalizedStatus);
        
        // Update Trello card position based on status
        if (request.getTrelloCardId() != null && !request.getTrelloCardId().isEmpty()) {
            String targetListId = null;
            if (normalizedStatus.equals(STATUS_IN_PROGRESS) || normalizedStatus.equals(STATUS_PENDING)) {
                targetListId = trelloService.getInProgressListId();
            } else if (normalizedStatus.equals(STATUS_DONE)) {
                targetListId = trelloService.getDoneListId();
            }
            
            if (targetListId != null) {
                trelloService.moveCardToList(request.getTrelloCardId(), targetListId);
            }
        }
        
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        log.info("Admin {} updated maintenance request {} status from {} to {}", adminId, requestId, oldStatus, normalizedStatus);
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
        
        // Create Trello card if not exists
        if (request.getTrelloCardId() == null || request.getTrelloCardId().isEmpty()) {
            String cardTitle = String.format("[MR-%s] %s", request.getId().toString().substring(0, 8), request.getTitle());
            String cardDescription = buildTrelloCardDescription(request);
            String cardId = trelloService.createCard(request.getId(), cardTitle, cardDescription, trelloService.getInProgressListId());
            if (cardId != null) {
                request.setTrelloCardId(cardId);
            }
        } else {
            // Move card to IN_PROGRESS list
            trelloService.moveCardToList(request.getTrelloCardId(), trelloService.getInProgressListId());
        }
        
        // Assign card to staff if staff ID is set
        if (request.getAssignedStaffId() != null && request.getTrelloCardId() != null && !request.getTrelloCardId().isEmpty()) {
            assignTrelloCardToStaff(request.getTrelloCardId(), request.getAssignedStaffId());
        }
        
        MaintenanceRequest saved = maintenanceRequestRepository.save(request);
        notifyMaintenanceResponseApproved(saved);
        log.info("Resident {} approved response for maintenance request {}, status changed to IN_PROGRESS", resident.getId(), requestId);
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
        notifyMaintenanceCancelled(saved);
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

    private String buildTrelloCardDescription(MaintenanceRequest request) {
        StringBuilder desc = new StringBuilder();
        desc.append("**Mô tả:** ").append(request.getDescription()).append("\n\n");
        desc.append("**Địa điểm:** ").append(request.getLocation()).append("\n");
        desc.append("**Danh mục:** ").append(request.getCategory()).append("\n");
        desc.append("**Liên hệ:** ").append(request.getContactName()).append(" - ").append(request.getContactPhone()).append("\n");
        
        if (request.getPreferredDatetime() != null) {
            desc.append("**Thời gian mong muốn:** ").append(request.getPreferredDatetime()).append("\n");
        }
        
        if (request.getEstimatedCost() != null) {
            desc.append("**Chi phí ước tính:** ").append(String.format("%,.0f", request.getEstimatedCost())).append(" VNĐ\n");
        }
        
        if (request.getAdminResponse() != null && !request.getAdminResponse().isEmpty()) {
            desc.append("\n**Phản hồi từ admin:** ").append(request.getAdminResponse()).append("\n");
        }
        
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            desc.append("\n**Ghi chú:** ").append(request.getNote()).append("\n");
        }
        
        desc.append("\n**Request ID:** ").append(request.getId().toString());
        
        return desc.toString();
    }

    /**
     * Assign Trello card to staff member by getting email from IAM service
     * @param cardId Trello card ID
     * @param staffId Staff user ID
     */
    private void assignTrelloCardToStaff(String cardId, UUID staffId) {
        try {
            // Get staff email from IAM service
            var accountInfo = iamClientService.getUserAccountInfo(staffId);
            if (accountInfo == null) {
                log.warn("Could not find user account info for staff ID: {}", staffId);
                return;
            }

            String staffEmail = accountInfo.email();
            if (staffEmail == null || staffEmail.isEmpty()) {
                log.warn("Staff ID {} has no email address", staffId);
                return;
            }

            // Assign member to Trello card using email
            boolean assigned = trelloService.assignMemberToCard(cardId, staffEmail);
            if (assigned) {
                log.info("Successfully assigned staff {} (email: {}) to Trello card {}", staffId, staffEmail, cardId);
            } else {
                log.warn("Failed to assign staff {} (email: {}) to Trello card {}", staffId, staffEmail, cardId);
            }
        } catch (Exception e) {
            log.error("Error assigning Trello card {} to staff {}: {}", cardId, staffId, e.getMessage(), e);
        }
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

    /**
     * Handle Trello webhook to sync status from Trello to database
     * @param payload Webhook payload from Trello
     */
    @SuppressWarnings("unchecked")
    public void handleTrelloWebhook(java.util.Map<String, Object> payload) {
        try {
            log.debug("Received Trello webhook payload: {}", payload);
            
            // Trello webhook structure: { "action": {...}, "model": {...} }
            java.util.Map<String, Object> action = (java.util.Map<String, Object>) payload.get("action");
            if (action == null) {
                log.warn("Trello webhook payload missing action field");
                return;
            }

            String actionType = (String) action.get("type");
            if (actionType == null) {
                log.warn("Trello webhook action missing type field");
                return;
            }

            log.debug("Trello webhook action type: {}", actionType);

            // Handle card movement events (updateCard with listAfter)
            if (!"updateCard".equals(actionType)) {
                log.debug("Ignoring Trello webhook action type: {}", actionType);
                return;
            }

            java.util.Map<String, Object> data = (java.util.Map<String, Object>) action.get("data");
            if (data == null) {
                log.warn("Trello webhook action missing data field");
                return;
            }

            // Check if card was moved to a different list
            java.util.Map<String, Object> listAfter = (java.util.Map<String, Object>) data.get("listAfter");
            if (listAfter == null) {
                log.debug("Trello webhook: card update but not moved to different list (no listAfter)");
                return;
            }

            java.util.Map<String, Object> card = (java.util.Map<String, Object>) data.get("card");
            if (card == null) {
                log.warn("Trello webhook missing card information");
                return;
            }

            String cardId = (String) card.get("id");
            String listId = (String) listAfter.get("id");
            
            if (cardId == null || listId == null) {
                log.warn("Trello webhook missing cardId or listId. cardId: {}, listId: {}", cardId, listId);
                return;
            }

            log.info("Trello webhook: card {} moved to list {}", cardId, listId);

            // Find maintenance request by Trello card ID
            MaintenanceRequest request = maintenanceRequestRepository.findByTrelloCardId(cardId);
            if (request == null) {
                log.debug("No maintenance request found for Trello card ID: {}", cardId);
                return;
            }

            // Map list ID to status
            String newStatus = mapListIdToStatus(listId);
            if (newStatus == null) {
                log.warn("Unknown Trello list ID: {}. Known lists: todo={}, inprogress={}, done={}", 
                        listId, todoListId, inProgressListId, doneListId);
                return;
            }

            // Only update if status changed
            if (!newStatus.equalsIgnoreCase(request.getStatus())) {
                String oldStatus = request.getStatus();
                request.setStatus(newStatus);
                maintenanceRequestRepository.save(request);
                log.info("✅ Updated maintenance request {} status from {} to {} via Trello webhook", 
                        request.getId(), oldStatus, newStatus);
            } else {
                log.debug("Maintenance request {} status unchanged: {}", request.getId(), newStatus);
            }
        } catch (ClassCastException e) {
            log.error("Error parsing Trello webhook payload - type mismatch: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error handling Trello webhook: {}", e.getMessage(), e);
            // Don't throw exception to avoid webhook retry loop
        }
    }

    /**
     * Map Trello list ID to maintenance request status
     * @param listId Trello list ID
     * @return Status string or null if unknown
     */
    private String mapListIdToStatus(String listId) {
        if (listId == null) return null;
        
        if (listId.equals(todoListId)) {
            return STATUS_PENDING;
        } else if (listId.equals(inProgressListId)) {
            return STATUS_IN_PROGRESS;
        } else if (listId.equals(doneListId)) {
            return STATUS_DONE;
        }
        
        return null;
    }
}

