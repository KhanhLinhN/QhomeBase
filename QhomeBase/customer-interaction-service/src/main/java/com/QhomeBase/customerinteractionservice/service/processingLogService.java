package com.QhomeBase.customerinteractionservice.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class processingLogService {

   private final processingLogRepository processingLogRepository;
    private final requestRepository requestRepository;

   public processingLogService(processingLogRepository processingLogRepository, requestRepository requestRepository) {
       this.processingLogRepository = processingLogRepository;
       this.requestRepository = requestRepository;
   }

    private ProcessingLogDTO mapToDto(ProcessingLog entity) {
        return new ProcessingLogDTO(
            entity.getId(),
            entity.getRecordType(),
            entity.getRecordId(),
            entity.getStaffInCharge(),
            entity.getContent(),
            entity.getRequestStatus(),
            entity.getLogType(),
            entity.getStaffInChargeName(),
            entity.getCreatedAt().toString().replace("T", " ")
        );
    }

    // Get processing logs by recordId
    public List<ProcessingLogDTO> getProcessingLogsById(UUID recordId) {
       return processingLogRepository.findByRecordIdOrderByCreatedAtDesc(recordId).stream().map(this::mapToDto).toList();
    }

    // Get processing logs by processing log id
    public List<ProcessingLogDTO> getProcessingLogsByLogsId(UUID logsId) {
        return processingLogRepository.findById(logsId).stream().map(this::mapToDto).toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByStaffId(UUID staffId) {
        return processingLogRepository.findByStaffInCharge(staffId).stream().map(this::mapToDto).toList();
    }

    // Add a new processing log
    @Transactional
    public ProcessingLogDTO addProcessingLog(UUID id, ProcessingLogDTO dto) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));
        String newStatus = dto.getRequestStatus();
        request.setStatus(newStatus);
        requestRepository.save(request);
        ProcessingLog entity = new ProcessingLog();
        entity.setRecordType(dto.getRecordType());
        entity.setRecordId(id);
        entity.setStaffInCharge(dto.getStaffInCharge());
        entity.setContent(dto.getContent());
        entity.setRequestStatus(newStatus);
        entity.setLogType(dto.getLogType());
        entity.setStaffInChargeName(dto.getStaffInChargeName());
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(entity);
        return mapToDto(entity);

    }


}
