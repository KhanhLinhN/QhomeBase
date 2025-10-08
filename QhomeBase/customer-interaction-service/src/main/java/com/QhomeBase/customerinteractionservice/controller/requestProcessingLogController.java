package com.QhomeBase.customerinteractionservice.controller;

//import java.util.UUID;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.service.processingLogService;

@RestController
@RequestMapping("/api/requests-logs")
public class requestProcessingLogController {
    private final processingLogService processingLogService;
    public requestProcessingLogController(processingLogService processingLogService) {
        this.processingLogService = processingLogService;
    }

//     @GetMapping("/{id}")
//     public ResponseEntity<ProcessingLogDTO> getProcessingLog(@RequestParam UUID id) {
//         ProcessingLogDTO processingLog = processingLogService.getProcessingLogsById(id);
//         if (processingLog != null) {
//             return ResponseEntity.ok(processingLog);
//         } else {
//             return ResponseEntity.notFound().build();
//         }
//     }
}
