package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.IamClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final IamClientService iamClientService;
    private final ResidentRepository residentRepository;
    
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            log.debug("Getting current user info for userId: {}", userId);
            
            // Get user account info from IAM service
            log.debug("Calling IAM service to get user account info for userId: {}", userId);
            ResidentAccountDto accountDto = iamClientService.getUserAccountInfo(userId);
            if (accountDto == null) {
                log.warn("User account not found in IAM service for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            log.debug("Retrieved user account info: username={}, email={}, roles={}", 
                    accountDto.username(), accountDto.email(), accountDto.roles());
            
            // Get resident info if exists
            UUID residentId = null;
            String fullName = null;
            try {
                var residentOpt = residentRepository.findByUserId(userId);
                if (residentOpt.isPresent()) {
                    var resident = residentOpt.get();
                    residentId = resident.getId();
                    fullName = resident.getFullName();
                }
                log.debug("Resident info for userId {}: residentId={}, fullName={}", userId, residentId, fullName);
            } catch (Exception e) {
                log.warn("Error finding resident for userId {}: {}", userId, e.getMessage());
                // Continue without resident info
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", userId.toString());
            response.put("username", accountDto.username());
            response.put("email", accountDto.email());
            response.put("roles", accountDto.roles());
            response.put("active", accountDto.active());
            if (residentId != null) {
                response.put("residentId", residentId.toString());
            }
            if (fullName != null) {
                response.put("fullName", fullName);
            }
            
            log.debug("Returning user info response for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Runtime error getting current user: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get user info: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error getting current user: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}

