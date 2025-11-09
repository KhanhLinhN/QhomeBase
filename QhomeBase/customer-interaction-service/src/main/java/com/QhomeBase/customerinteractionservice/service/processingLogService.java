package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.IamServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.IamUserInfoResponse;
import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class processingLogService {

    private final processingLogRepository processingLogRepository;
    private final requestRepository requestRepository;
    private final IamServiceClient iamServiceClient;

    public processingLogService(processingLogRepository processingLogRepository,
                                requestRepository requestRepository,
                                IamServiceClient iamServiceClient) {
        this.processingLogRepository = processingLogRepository;
        this.requestRepository = requestRepository;
        this.iamServiceClient = iamServiceClient;
    }

    private ProcessingLogDTO mapToDto(ProcessingLog entity) {
        String staffName = entity.getStaffInChargeName();
        String staffEmail = null;
        if (entity.getStaffInCharge() != null) {
            IamUserInfoResponse info = iamServiceClient.fetchUserInfo(entity.getStaffInCharge());
            if (info != null) {
                if (!StringUtils.hasText(staffName)) {
                    staffName = info.username();
                }
                staffEmail = info.email();
            }
        }
        return new ProcessingLogDTO(
                entity.getId(),
                entity.getRecordId(),
                entity.getContent(),
                entity.getRequestStatus(),
                staffName,
                staffEmail,
                entity.getCreatedAt().toString().replace("T", " ")
        );
    }

    public List<ProcessingLogDTO> getProcessingLogsById(UUID recordId) {
        return processingLogRepository.findByRecordIdOrderByCreatedAtDesc(recordId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByLogsId(UUID logsId) {
        return processingLogRepository.findById(logsId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByStaffId(UUID staffId) {
        return processingLogRepository.findByStaffInCharge(staffId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public ProcessingLogDTO addProcessingLog(UUID id, ProcessingLogDTO dto) {
        return addProcessingLog(id, dto, null);
    }

    @Transactional
    public ProcessingLogDTO addProcessingLog(UUID id, ProcessingLogDTO dto, Authentication authentication) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));

        if ("Done".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Request has been completed and cannot be updated");
        }

        String newStatus = StringUtils.hasText(dto.getRequestStatus())
                ? dto.getRequestStatus()
                : request.getStatus();
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        requestRepository.save(request);

        UUID staffId = resolveStaffId(authentication);
        String staffName = resolveStaffName(staffId);

        ProcessingLog entity = new ProcessingLog();
        entity.setRecordId(id);
        entity.setStaffInCharge(staffId);
        entity.setContent(dto.getContent());
        entity.setRequestStatus(newStatus);
        entity.setStaffInChargeName(staffName);
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(entity);
        return mapToDto(entity);
    }

    private UUID resolveStaffId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.uid();
        }
        return null;
    }

    private String resolveStaffName(UUID staffId) {
        if (staffId == null) {
            return null;
        }
        IamUserInfoResponse info = iamServiceClient.fetchUserInfo(staffId);
        if (info == null) {
            return null;
        }
        if (StringUtils.hasText(info.username())) {
            return info.username();
        }
        return info.email();
    }
}
