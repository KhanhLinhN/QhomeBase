package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CleaningRequestDto;
import com.QhomeBase.baseservice.dto.CreateCleaningRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.CleaningRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cleaning-requests")
@RequiredArgsConstructor
@Slf4j
public class CleaningRequestController {

    private final CleaningRequestService cleaningRequestService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCleaningRequestDto requestDto) {
        try {
            CleaningRequestDto created = cleaningRequestService.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to create cleaning request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}

