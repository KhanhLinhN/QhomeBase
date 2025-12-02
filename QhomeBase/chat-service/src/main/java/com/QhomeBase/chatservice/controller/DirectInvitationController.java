package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.CreateDirectInvitationRequest;
import com.QhomeBase.chatservice.dto.DirectInvitationResponse;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.DirectInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct-invitations")
@RequiredArgsConstructor
@Tag(name = "Direct Invitations", description = "APIs for direct chat invitations")
public class DirectInvitationController {

    private final DirectInvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Create invitation", description = "Create a direct chat invitation")
    public ResponseEntity<DirectInvitationResponse> createInvitation(
            @RequestBody CreateDirectInvitationRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectInvitationResponse invitation = invitationService.createInvitation(userId, request);
        return ResponseEntity.ok(invitation);
    }

    @PostMapping("/{invitationId}/accept")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Accept invitation", description = "Accept a direct chat invitation")
    public ResponseEntity<DirectInvitationResponse> acceptInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectInvitationResponse invitation = invitationService.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok(invitation);
    }

    @PostMapping("/{invitationId}/decline")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Decline invitation", description = "Decline a direct chat invitation")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        invitationService.declineInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get pending invitations", description = "Get all pending invitations for current user")
    public ResponseEntity<List<DirectInvitationResponse>> getPendingInvitations(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        List<DirectInvitationResponse> invitations = invitationService.getPendingInvitations(userId);
        return ResponseEntity.ok(invitations);
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Count pending invitations", description = "Get count of pending invitations")
    public ResponseEntity<Long> countPendingInvitations(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        Long count = invitationService.countPendingInvitations(userId);
        return ResponseEntity.ok(count);
    }
}

