package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.GroupInvitationResponse;
import com.QhomeBase.chatservice.dto.InviteMembersByPhoneRequest;
import com.QhomeBase.chatservice.dto.InviteMembersResponse;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.GroupInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Group Invitations", description = "Group invitation management APIs")
public class GroupInvitationController {

    private final GroupInvitationService invitationService;

    @PostMapping("/{groupId}/invite")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Invite members by phone number", description = "Invite members to group using phone numbers. Validates that phone numbers exist in the system.")
    public ResponseEntity<InviteMembersResponse> inviteMembersByPhone(
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteMembersByPhoneRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        InviteMembersResponse response = invitationService.inviteMembersByPhone(groupId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitations/my")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my pending invitations", description = "Get list of pending group invitations for current user")
    public ResponseEntity<List<GroupInvitationResponse>> getMyPendingInvitations(
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        List<GroupInvitationResponse> response = invitationService.getMyPendingInvitations(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Accept invitation", description = "Accept a group invitation")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        invitationService.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Decline invitation", description = "Decline a group invitation")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        invitationService.declineInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }
}

