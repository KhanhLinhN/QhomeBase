package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionRequest;
import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionResponse;
import com.QhomeBase.baseservice.exception.GlobalExceptionHandler;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.AccountProvideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/units")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class AccountProvideController {

    private final AccountProvideService accountProvideService;

    @PostMapping("/{unitId}/primary-resident/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> provisionPrimaryResident(
            @PathVariable UUID unitId,
            @Valid @RequestBody PrimaryResidentProvisionRequest request,
            Authentication authentication
    ) {
        try {
            Object principalObj = authentication != null ? authentication.getPrincipal() : null;
            UserPrincipal principal = principalObj instanceof UserPrincipal ? (UserPrincipal) principalObj : null;
            String token = principal != null ? principal.token() : null;
            PrimaryResidentProvisionResponse response = accountProvideService.provisionPrimaryResident(
                    unitId,
                    request,
                    token
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to provision primary resident for unit {}: {}", unitId, e.getMessage());
            // Return proper error response with message
            GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage() != null ? e.getMessage() : "Failed to provision primary resident",
                    Instant.now()
            );
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error while provisioning primary resident for unit {}", unitId, e);
            // Return proper error response with message
            GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    e.getMessage() != null ? e.getMessage() : "Internal server error",
                    Instant.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

