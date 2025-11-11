package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.ImportedReadingDto;
import com.QhomeBase.financebillingservice.dto.MeterReadingImportResponse;
import com.QhomeBase.financebillingservice.service.MeterReadingImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Slf4j
public class MeterReadingImportController {

    private final MeterReadingImportService importService;

    @PostMapping("/import")
    public ResponseEntity<MeterReadingImportResponse> importReadings(@RequestBody List<ImportedReadingDto> readings) {
        int count = readings != null ? readings.size() : 0;
        log.info("Received {} imported readings for invoicing", count);
        
        MeterReadingImportResponse response = importService.importReadingsWithResponse(readings);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


