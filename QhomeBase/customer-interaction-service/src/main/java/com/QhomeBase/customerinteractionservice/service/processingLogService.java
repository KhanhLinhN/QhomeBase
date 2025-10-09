package com.QhomeBase.customerinteractionservice.service;

import java.util.List;
import java.util.UUID;

import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import org.springframework.stereotype.Service;

@Service
public class processingLogService {

   private final processingLogRepository processingLogRepository;

   public processingLogService(processingLogRepository processingLogRepository) {
       this.processingLogRepository = processingLogRepository;
   }

    private ProcessingLogDTO mapToDto(ProcessingLog entity) {
        return new ProcessingLogDTO(
            entity.getId(),
            entity.getRecord_type(),
            entity.getRecord_id(),
            entity.getStaff_in_charge(),
            entity.getContent(),
            entity.getRequest_status(),
            entity.getLog_type(),
            entity.getStaff_in_charge_name(),
            entity.getCreated_at()
        );          
    }

    // Get processing logs by record_id
    public List<ProcessingLogDTO> getProcessingLogsById(UUID record_id) {
       return processingLogRepository.findByRecord_id(record_id).stream().map(this::mapToDto).toList();
    }

    // Get processing logs by processing log id
    public List<ProcessingLogDTO> getProcessingLogsByLogsId(UUID logsId) {
        return processingLogRepository.findById(logsId).stream().map(this::mapToDto).toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByStaffId(UUID staffId) {
        return processingLogRepository.findByStaff_in_charge(staffId).stream().map(this::mapToDto).toList();
    }

    // Add a new processing log
    public void addProcessingLog(ProcessingLogDTO dto) {
        ProcessingLog entity = new ProcessingLog();
        entity.setId(dto.getId());
        entity.setRecord_type(dto.getRecord_type());
        entity.setRecord_id(dto.getRecord_id());
        entity.setStaff_in_charge(dto.getStaff_in_charge());
        entity.setContent(dto.getContent());
        entity.setRequest_status(dto.getRequest_status());
        entity.setLog_type(dto.getLog_type());
        entity.setStaff_in_charge_name(dto.getStaff_in_charge_name());
        entity.setCreated_at(dto.getCreated_at());
        processingLogRepository.save(entity);
    }

    //

}
