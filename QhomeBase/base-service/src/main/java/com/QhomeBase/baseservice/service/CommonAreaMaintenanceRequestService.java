package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.CommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.CreateCommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.model.CommonAreaMaintenanceRequest;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.CommonAreaMaintenanceRequestRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CommonAreaMaintenanceRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REJECTED = "REJECTED";
    
    private static final String RESPONSE_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String RESPONSE_STATUS_APPROVED = "APPROVED";
    private static final String RESPONSE_STATUS_REJECTED = "REJECTED";

    private final CommonAreaMaintenanceRequestRepository repository;
    private final ResidentRepository residentRepository;
    private final NotificationClient notificationClient;
    private final EntityManager entityManager;

    public CommonAreaMaintenanceRequestService(
            CommonAreaMaintenanceRequestRepository repository,
            ResidentRepository residentRepository,
            NotificationClient notificationClient,
            EntityManager entityManager) {
        this.repository = repository;
        this.residentRepository = residentRepository;
        this.notificationClient = notificationClient;
        this.entityManager = entityManager;
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto create(UUID userId, CreateCommonAreaMaintenanceRequestDto dto) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));

        // Validate no active request (similar to unit maintenance)
        ensureNoActiveRequest(resident.getId());

        List<String> attachments = dto.attachments() != null
                ? new ArrayList<>(dto.attachments())
                : new ArrayList<>();

        if (attachments.size() > 5) {
            throw new IllegalArgumentException("Only up to 5 attachments are allowed");
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

        CommonAreaMaintenanceRequest request = CommonAreaMaintenanceRequest.builder()
                .id(UUID.randomUUID())
                .buildingId(dto.buildingId())
                .residentId(resident.getId())
                .createdBy(userId)
                .userId(userId)
                .areaType(dto.areaType().trim())
                .title(dto.title().trim())
                .description(dto.description().trim())
                .attachments(attachments)
                .location(dto.location().trim())
                .contactName(contactName)
                .contactPhone(contactPhone)
                .note(dto.note())
                .status(STATUS_PENDING)
                .build();

        // Use persist() for new entity to avoid merge issues
        entityManager.persist(request);
        entityManager.flush();
        CommonAreaMaintenanceRequest saved = request;
        log.info("Created common area maintenance request {} by resident {}", saved.getId(), resident.getId());
        
        // Notify admin about new common area maintenance request
        notifyAdminNewRequest(saved);
        
        return toDto(saved);
    }

    private void ensureNoActiveRequest(UUID residentId) {
        List<String> excludedStatuses = List.of(STATUS_COMPLETED, STATUS_CANCELLED, STATUS_REJECTED);
        List<CommonAreaMaintenanceRequest> activeRequests = repository
                .findByResidentIdAndStatusNotIn(residentId, excludedStatuses);
        
        if (!activeRequests.isEmpty()) {
            throw new IllegalStateException(
                    "Bạn đang có yêu cầu bảo trì khu vực chung chưa được xử lý. Vui lòng đợi yêu cầu hiện tại được hoàn thành hoặc hủy trước khi tạo yêu cầu mới."
            );
        }
    }

    private void notifyAdminNewRequest(CommonAreaMaintenanceRequest request) {
        // Note: Admin notifications are typically handled via internal notification system
        // For now, we log the event. Admin can view requests via API endpoints
        log.info("New common area maintenance request created: {} by resident {}", 
                request.getId(), request.getResidentId());
    }

    public List<CommonAreaMaintenanceRequestDto> getMyRequests(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        List<CommonAreaMaintenanceRequest> requests = repository
                .findByResidentIdOrderByCreatedAtDesc(resident.getId());
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CommonAreaMaintenanceRequestDto> getPendingRequests() {
        List<CommonAreaMaintenanceRequest> requests = repository
                .findByStatusOrderByCreatedAtDesc(STATUS_PENDING);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CommonAreaMaintenanceRequestDto> getInProgressRequests() {
        List<CommonAreaMaintenanceRequest> requests = repository
                .findByStatusOrderByCreatedAtDesc(STATUS_IN_PROGRESS);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CommonAreaMaintenanceRequestDto> getRequestsByStatus(String status) {
        List<CommonAreaMaintenanceRequest> requests = repository
                .findByStatusOrderByCreatedAtDesc(status);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CommonAreaMaintenanceRequestDto> getAllRequests() {
        List<CommonAreaMaintenanceRequest> requests = repository.findAll();
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto getRequestById(UUID requestId) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found with id: " + requestId));
        return toDto(request);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto respondToRequest(
            UUID adminId,
            UUID requestId,
            AdminMaintenanceResponseDto dto) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be responded to");
        }

        request.setAdminResponse(dto.adminResponse());
        request.setEstimatedCost(dto.estimatedCost());
        request.setRespondedBy(adminId);
        request.setRespondedAt(OffsetDateTime.now());
        request.setResponseStatus(RESPONSE_STATUS_PENDING_APPROVAL);
        request.setStatus(STATUS_PENDING); // Keep as PENDING until resident approves

        CommonAreaMaintenanceRequest saved = repository.save(request);
        notifyResidentResponse(saved);
        log.info("Admin {} responded to common area maintenance request {}", adminId, requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto denyRequest(
            UUID adminId,
            UUID requestId,
            AdminServiceRequestActionDto dto) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be denied");
        }

        request.setStatus(STATUS_REJECTED);
        request.setAdminResponse(dto.note());
        request.setRespondedBy(adminId);
        request.setRespondedAt(OffsetDateTime.now());

        CommonAreaMaintenanceRequest saved = repository.save(request);
        notifyResidentDenied(saved);
        log.info("Admin {} denied common area maintenance request {}", adminId, requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto completeRequest(
            UUID adminId,
            UUID requestId,
            AdminServiceRequestActionDto dto) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only in-progress requests can be completed");
        }

        request.setStatus(STATUS_COMPLETED);
        request.setCompletedAt(OffsetDateTime.now());
        if (dto != null && StringUtils.hasText(dto.note())) {
            String existingNotes = request.getProgressNotes();
            String newNote = StringUtils.hasText(existingNotes)
                    ? existingNotes + "\n" + dto.note()
                    : dto.note();
            request.setProgressNotes(newNote);
        }

        CommonAreaMaintenanceRequest saved = repository.save(request);
        notifyResidentCompleted(saved);
        log.info("Admin {} completed common area maintenance request {}", adminId, requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto approveResponse(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

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

        CommonAreaMaintenanceRequest saved = repository.save(request);
        log.info("Resident {} approved response for common area maintenance request {}", 
                resident.getId(), requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto rejectResponse(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only reject responses for your own requests");
        }

        if (!RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            throw new IllegalStateException("No pending response to reject");
        }

        request.setResponseStatus(RESPONSE_STATUS_REJECTED);
        request.setStatus(STATUS_CANCELLED);
        CommonAreaMaintenanceRequest saved = repository.save(request);
        notifyResidentResponseRejected(saved);
        log.info("Resident {} rejected response for common area maintenance request {}", 
                resident.getId(), requestId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto cancelRequest(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (STATUS_COMPLETED.equalsIgnoreCase(request.getStatus()) ||
                STATUS_CANCELLED.equalsIgnoreCase(request.getStatus()) ||
                STATUS_REJECTED.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Cannot cancel a completed, rejected, or already cancelled request");
        }

        request.setStatus(STATUS_CANCELLED);
        if (request.getResponseStatus() == null || 
                RESPONSE_STATUS_PENDING_APPROVAL.equalsIgnoreCase(request.getResponseStatus())) {
            request.setResponseStatus(RESPONSE_STATUS_REJECTED);
        }
        CommonAreaMaintenanceRequest saved = repository.save(request);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto assignToStaff(UUID adminId, UUID requestId, UUID staffId) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus()) && 
                !STATUS_IN_PROGRESS.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Can only assign pending or in-progress requests");
        }

        request.setAssignedTo(staffId);
        if (STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            request.setStatus(STATUS_IN_PROGRESS);
        }

        CommonAreaMaintenanceRequest saved = repository.save(request);
        log.info("Admin {} assigned common area maintenance request {} to staff {}", 
                adminId, requestId, staffId);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CommonAreaMaintenanceRequestDto addProgressNote(UUID adminId, UUID requestId, String note) {
        CommonAreaMaintenanceRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Common area maintenance request not found"));

        String existingNotes = request.getProgressNotes();
        String newNote = StringUtils.hasText(existingNotes)
                ? existingNotes + "\n" + note
                : note;
        request.setProgressNotes(newNote);

        CommonAreaMaintenanceRequest saved = repository.save(request);
        return toDto(saved);
    }

    private void notifyResidentResponse(CommonAreaMaintenanceRequest request) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [CommonAreaMaintenanceRequest] Missing residentId for request {}", request.getId());
            return;
        }

        StringBuilder body = new StringBuilder("Yêu cầu bảo trì khu vực chung \"")
                .append(request.getTitle())
                .append("\" đã nhận được phản hồi từ admin.");
        
        if (request.getEstimatedCost() != null) {
            body.append(" Chi phí ước tính: ")
                    .append(String.format("%,.0f", request.getEstimatedCost()))
                    .append(" VNĐ.");
        }
        body.append(" Vui lòng xem chi tiết và xác nhận.");

        Map<String, String> data = new HashMap<>();
        data.put("entity", "COMMON_AREA_MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", request.getStatus());
        data.put("areaType", request.getAreaType());

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                request.getBuildingId(),
                "REQUEST",
                "Phản hồi yêu cầu bảo trì khu vực chung",
                body.toString(),
                request.getId(),
                "COMMON_AREA_MAINTENANCE_REQUEST",
                data
        );
    }

    private void notifyResidentDenied(CommonAreaMaintenanceRequest request) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [CommonAreaMaintenanceRequest] Missing residentId for request {}", request.getId());
            return;
        }

        StringBuilder body = new StringBuilder("Yêu cầu bảo trì khu vực chung \"")
                .append(request.getTitle())
                .append("\" đã bị từ chối bởi admin.");
        
        if (request.getAdminResponse() != null && !request.getAdminResponse().isBlank()) {
            body.append(" ").append(request.getAdminResponse());
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "COMMON_AREA_MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", STATUS_REJECTED);
        data.put("areaType", request.getAreaType());

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                request.getBuildingId(),
                "REQUEST",
                "Yêu cầu bảo trì khu vực chung bị từ chối",
                body.toString(),
                request.getId(),
                "COMMON_AREA_MAINTENANCE_REQUEST",
                data
        );
    }

    private void notifyResidentCompleted(CommonAreaMaintenanceRequest request) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [CommonAreaMaintenanceRequest] Missing residentId for request {}", request.getId());
            return;
        }

        StringBuilder body = new StringBuilder("Yêu cầu bảo trì khu vực chung \"")
                .append(request.getTitle())
                .append("\" đã hoàn tất.");
        
        if (request.getProgressNotes() != null && !request.getProgressNotes().isBlank()) {
            body.append(" ").append(request.getProgressNotes());
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "COMMON_AREA_MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", STATUS_COMPLETED);
        data.put("areaType", request.getAreaType());

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                request.getBuildingId(),
                "REQUEST",
                "Yêu cầu bảo trì khu vực chung đã hoàn thành",
                body.toString(),
                request.getId(),
                "COMMON_AREA_MAINTENANCE_REQUEST",
                data
        );
    }

    private void notifyResidentResponseRejected(CommonAreaMaintenanceRequest request) {
        // Log for admin awareness - admin can view via API
        log.info("Resident {} rejected response for common area maintenance request {}", 
                request.getResidentId(), request.getId());
    }

    private CommonAreaMaintenanceRequestDto toDto(CommonAreaMaintenanceRequest entity) {
        return new CommonAreaMaintenanceRequestDto(
                entity.getId(),
                entity.getBuildingId(),
                entity.getResidentId(),
                entity.getUserId(),
                entity.getCreatedBy(),
                entity.getAreaType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getAttachments(),
                entity.getLocation(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getNote(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAdminResponse(),
                entity.getEstimatedCost(),
                entity.getRespondedBy(),
                entity.getRespondedAt(),
                entity.getResponseStatus(),
                entity.getProgressNotes(),
                entity.getAssignedTo(),
                entity.getCompletedAt()
        );
    }
}
