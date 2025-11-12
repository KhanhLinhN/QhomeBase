package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterReadingImportResponse;
import com.QhomeBase.baseservice.service.MeterReadingExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/meter-readings/export")
@RequiredArgsConstructor
@Slf4j
public class MeterReadingExportController {

    private final MeterReadingExportService exportService;

    @PostMapping("/cycle/{cycleId}")
    public ResponseEntity<MeterReadingImportResponse> exportByCycle(@PathVariable UUID cycleId) {
        try {
            log.info("Received export request for cycle {}", cycleId);
            MeterReadingImportResponse response = exportService.exportReadingsByCycle(cycleId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to export readings from cycle: {}", cycleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MeterReadingImportResponse.builder()
                            .totalReadings(0)
                            .invoicesCreated(0)
                            .message("Failed to export readings: " + e.getMessage())
                            .build());
        }
    }
}
