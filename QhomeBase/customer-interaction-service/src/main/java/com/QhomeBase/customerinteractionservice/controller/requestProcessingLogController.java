package com.QhomeBase.customerinteractionservice.controller;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.service.processingLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/requests-logs")
public class requestProcessingLogController {
    private final processingLogService processingLogService;
    public requestProcessingLogController(processingLogService processingLogService) {
        this.processingLogService = processingLogService;
    }

    @GetMapping("/{id}")
    public List<ProcessingLogDTO> getProcessingLog(@RequestParam UUID id) {
        return processingLogService.getProcessingLogsById(id);
    }

    @GetMapping("/{id}/logs")
    public List<ProcessingLogDTO> getProcessingLogsByLogsId(@PathVariable UUID id) {
        return processingLogService.getProcessingLogsById(id);
    }

    @GetMapping("/staff/{staffId}")
    public List<ProcessingLogDTO> getProcessingLogsByStaffId(@PathVariable UUID staffId) {
        return processingLogService.getProcessingLogsByStaffId(staffId);
    }

    @PostMapping("addNewLog")
    public ResponseEntity<ProcessingLogDTO> addNewProcessLog(@RequestBody ProcessingLogDTO entity) {
        processingLogService.addProcessingLog(entity);
        return ResponseEntity.ok(entity);
    }

    
    


}
