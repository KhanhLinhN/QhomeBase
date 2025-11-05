package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CreateResidentAccountDto;
import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.dto.ResidentWithoutAccountDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.ResidentAccountService;
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
@RequestMapping("/api/residents")
@RequiredArgsConstructor
@Slf4j
public class ResidentController {
    
    private final ResidentAccountService residentAccountService;
    
    /**
     * Get list of residents in household without account
     * GET /api/units/{unitId}/household/members/without-account
     */
    @GetMapping("/units/{unitId}/household/members/without-account")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<ResidentWithoutAccountDto>> getResidentsWithoutAccount(
            @PathVariable UUID unitId,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();
            
            List<ResidentWithoutAccountDto> residents = residentAccountService
                    .getResidentsWithoutAccount(unitId, requesterUserId);
            
            return ResponseEntity.ok(residents);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get residents without account: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create account for a resident
     * POST /api/residents/{residentId}/create-account
     */
    @PostMapping("/{residentId}/create-account")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ResidentAccountDto> createAccountForResident(
            @PathVariable UUID residentId,
            @Valid @RequestBody CreateResidentAccountDto request,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();
            
            ResidentAccountDto account = residentAccountService
                    .createAccountForResident(residentId, request, requesterUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create account for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get account info for a resident
     * GET /api/residents/{residentId}/account
     */
    @GetMapping("/{residentId}/account")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ResidentAccountDto> getResidentAccount(
            @PathVariable UUID residentId,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();
            
            ResidentAccountDto account = residentAccountService
                    .getResidentAccount(residentId, requesterUserId);
            
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get account for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

