package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdMemberRequestCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDecisionDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.HouseholdMemberRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/household-member-requests")
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberRequestController {

    private final HouseholdMemberRequestService requestService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<HouseholdMemberRequestDto> createRequest(
            @Valid @RequestBody HouseholdMemberRequestCreateDto createDto,
            Authentication authentication
    ) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            HouseholdMemberRequestDto dto = requestService.createRequest(createDto, principal);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create household member request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<HouseholdMemberRequestDto>> getMyRequests(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<HouseholdMemberRequestDto> requests = requestService.getRequestsForUser(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HouseholdMemberRequestDto>> getPendingRequests() {
        List<HouseholdMemberRequestDto> requests = requestService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HouseholdMemberRequestDto> decideRequest(
            @PathVariable UUID id,
            @Valid @RequestBody HouseholdMemberRequestDecisionDto decisionDto,
            Authentication authentication
    ) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            HouseholdMemberRequestDto dto = requestService.decideRequest(id, decisionDto, principal.uid());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to process household member request {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}





