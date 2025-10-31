package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterReadingCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingDto;
import com.QhomeBase.baseservice.service.MeterReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    public ResponseEntity<MeterReadingDto> create(@Valid @RequestBody MeterReadingCreateReq request,
                                                  Authentication authentication) {
        MeterReadingDto dto = meterReadingService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}


