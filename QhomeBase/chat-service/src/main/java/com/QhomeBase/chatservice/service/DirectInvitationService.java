package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.CreateDirectInvitationRequest;
import com.QhomeBase.chatservice.dto.DirectInvitationResponse;
import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.DirectInvitation;
import com.QhomeBase.chatservice.model.DirectMessage;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import com.QhomeBase.chatservice.repository.DirectInvitationRepository;
import com.QhomeBase.chatservice.repository.DirectMessageRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectInvitationService {

    private final DirectInvitationRepository invitationRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository messageRepository;
    private final BlockRepository blockRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final FriendshipService friendshipService;

    private String getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.token();
        }
        return null;
    }

    /**
     * Create a direct chat invitation
     * Rules:
     * - Inviter can only send ONE initial message
     * - Conversation is created with status PENDING
     * - Invitation expires after 7 days
     */
    @Transactional
    public DirectInvitationResponse createInvitation(
            UUID inviterId,
            CreateDirectInvitationRequest request) {
        
        String accessToken = getCurrentAccessToken();
        UUID inviterResidentId = residentInfoService.getResidentIdFromUserId(inviterId, accessToken);
        if (inviterResidentId == null) {
            throw new RuntimeException("Resident not found for user: " + inviterId);
        }

        // inviteeId from request might be userId or residentId
        // Try to convert from userId to residentId if needed
        UUID inviteeIdFromRequest = request.getInviteeId();
        UUID inviteeResidentId = residentInfoService.getResidentIdFromUserId(inviteeIdFromRequest, accessToken);
        if (inviteeResidentId == null) {
            // If conversion fails, assume it's already a residentId
            inviteeResidentId = inviteeIdFromRequest;
        }
        
        log.info("Checking block status: inviterResidentId={}, inviteeIdFromRequest={}, inviteeResidentId={}", 
                inviterResidentId, inviteeIdFromRequest, inviteeResidentId);

        // Check if users are blocked (bidirectional)
        boolean inviterBlockedInvitee = blockRepository
                .findByBlockerIdAndBlockedId(inviterResidentId, inviteeResidentId).isPresent();
        boolean inviteeBlockedInviter = blockRepository
                .findByBlockerIdAndBlockedId(inviteeResidentId, inviterResidentId).isPresent();
        
        log.info("Block check result: inviterBlockedInvitee={}, inviteeBlockedInviter={}", 
                inviterBlockedInvitee, inviteeBlockedInviter);
        
        if (inviterBlockedInvitee || inviteeBlockedInviter) {
            String message = inviterBlockedInvitee 
                ? "Cannot send invitation: You have blocked this user"
                : "Cannot send invitation: This user has blocked you";
            log.warn("Block check failed: {}", message);
            throw new RuntimeException(message);
        }

        // Ensure participant1_id < participant2_id for uniqueness
        UUID participant1Id = inviterResidentId.compareTo(inviteeResidentId) < 0 
            ? inviterResidentId 
            : inviteeResidentId;
        UUID participant2Id = inviterResidentId.compareTo(inviteeResidentId) < 0 
            ? inviteeResidentId 
            : inviterResidentId;

        // Check if conversation already exists
        Conversation conversation = conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id)
                .orElse(null);

        if (conversation != null) {
            // If conversation exists and is ACTIVE, invitation already accepted
            if ("ACTIVE".equals(conversation.getStatus())) {
                throw new RuntimeException("Conversation already exists and is active");
            }
            // If conversation status is BLOCKED, reset to PENDING to allow new invitations
            if ("BLOCKED".equals(conversation.getStatus())) {
                log.info("Conversation status is BLOCKED, resetting to PENDING: {}", conversation.getId());
                conversation.setStatus("PENDING");
                conversation = conversationRepository.save(conversation);
            }
        } else {
            // Create new conversation
            conversation = Conversation.builder()
                    .participant1Id(participant1Id)
                    .participant2Id(participant2Id)
                    .status("PENDING")
                    .createdBy(inviterResidentId)
                    .build();
            conversation = conversationRepository.save(conversation);

            // Create participants
            participantRepository.save(com.QhomeBase.chatservice.model.ConversationParticipant.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .residentId(participant1Id)
                    .build());
            participantRepository.save(com.QhomeBase.chatservice.model.ConversationParticipant.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .residentId(participant2Id)
                    .build());
        }

        log.info("=== createInvitation ===");
        log.info("InviterId (userId from JWT): {}", inviterId);
        log.info("InviterResidentId (converted): {}", inviterResidentId);
        log.info("InviteeId from request: {}", request.getInviteeId());
        log.info("InviteeResidentId (used): {}", inviteeResidentId);
        log.info("Conversation ID: {}", conversation.getId());
        log.info("Conversation Status: {}", conversation.getStatus());
        
        // Check if invitation already exists (any status) to avoid unique constraint violation
        // This checks for invitation from inviterResidentId to inviteeResidentId
        DirectInvitation existingInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviterResidentId, inviteeResidentId)
                .orElse(null);
        
        // Also check for reverse invitation (invitee invited inviter)
        Optional<DirectInvitation> reverseInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviteeResidentId, inviterResidentId);
        
        if (reverseInvitation.isPresent()) {
            log.info("Found reverse invitation ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                    reverseInvitation.get().getId(), 
                    reverseInvitation.get().getStatus(),
                    reverseInvitation.get().getInviterId(),
                    reverseInvitation.get().getInviteeId());
        } else {
            log.info("No reverse invitation found");
        }
        
        DirectInvitation invitation;
        if (existingInvitation != null) {
            log.info("Found existing invitation ID: {}, Status: {}", existingInvitation.getId(), existingInvitation.getStatus());
            
            // If invitation exists
            if ("PENDING".equals(existingInvitation.getStatus())) {
                // Check if there's a reverse invitation PENDING - if so, both users want to chat
                if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                    log.info("Found mutual invitations (both PENDING). Auto-accepting both invitations and creating friendship.");
                    
                    // Auto-accept existing invitation (A->B)
                    existingInvitation.setStatus("ACCEPTED");
                    existingInvitation.setRespondedAt(OffsetDateTime.now());
                    invitation = invitationRepository.save(existingInvitation);
                    log.info("Auto-accepted existing invitation ID: {}", invitation.getId());
                    
                    // Auto-accept reverse invitation (B->A)
                    DirectInvitation reverseInv = reverseInvitation.get();
                    reverseInv.setStatus("ACCEPTED");
                    reverseInv.setRespondedAt(OffsetDateTime.now());
                    invitationRepository.save(reverseInv);
                    log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                    
                    // Activate conversation
                    conversation.setStatus("ACTIVE");
                    conversation = conversationRepository.save(conversation);
                    log.info("Activated conversation ID: {}", conversation.getId());
                    
                    // Create or activate friendship between both users
                    friendshipService.createOrActivateFriendship(inviterResidentId, inviteeResidentId);
                    log.info("Friendship created/activated between {} and {} (mutual invitations - both PENDING)", inviterResidentId, inviteeResidentId);
                    
                    // Notify both users that conversation is now active
                    notificationService.notifyDirectInvitationAccepted(
                            reverseInv.getInviterId(),
                            conversation.getId(),
                            toResponse(reverseInv, accessToken));
                    notificationService.notifyDirectInvitationAccepted(
                            invitation.getInviterId(),
                            conversation.getId(),
                            toResponse(invitation, accessToken));
                } else if (!existingInvitation.isExpired()) {
                    // PENDING and not expired - return existing (no reverse invitation)
                    log.info("Invitation already exists and is pending: {}", existingInvitation.getId());
                    throw new RuntimeException("Invitation already exists and is pending");
                } else {
                    // PENDING but expired - update to new expiration
                    log.info("Invitation exists but expired, updating expiration: {}", existingInvitation.getId());
                    existingInvitation.setExpiresAt(OffsetDateTime.now().plusDays(7));
                    existingInvitation.setInitialMessage(request.getInitialMessage());
                    existingInvitation.setRespondedAt(null);
                    invitation = invitationRepository.save(existingInvitation);
                    log.info("Updated expired invitation ID: {}", invitation.getId());
                }
            } else if ("ACCEPTED".equals(existingInvitation.getStatus())) {
                // Already accepted - check conversation status
                if ("ACTIVE".equals(conversation.getStatus())) {
                    // Conversation is ACTIVE - invitation already accepted
                    log.info("Invitation already accepted and conversation is ACTIVE. Invitation ID: {}", existingInvitation.getId());
                    throw new RuntimeException("Conversation already exists and is active");
                } else {
                    // Data inconsistency: invitation ACCEPTED but conversation not ACTIVE
                    // Check if there's a reverse invitation PENDING - if so, auto-accept it and activate conversation
                    if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                        log.info("Invitation ACCEPTED but conversation PENDING. Found reverse invitation PENDING. Auto-accepting reverse invitation and activating conversation.");
                        
                        // Auto-accept reverse invitation
                        DirectInvitation reverseInv = reverseInvitation.get();
                        reverseInv.setStatus("ACCEPTED");
                        reverseInv.setRespondedAt(OffsetDateTime.now());
                        invitationRepository.save(reverseInv);
                        log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                        
                        // Activate conversation
                        conversation.setStatus("ACTIVE");
                        conversation = conversationRepository.save(conversation);
                        log.info("Activated conversation ID: {}", conversation.getId());
                        
                        // Create or activate friendship between both users
                        friendshipService.createOrActivateFriendship(inviterResidentId, inviteeResidentId);
                        log.info("Friendship created/activated between {} and {} (invitation accepted with reverse pending)", inviterResidentId, inviteeResidentId);
                        
                        // Notify both users that conversation is now active
                        notificationService.notifyDirectInvitationAccepted(
                                reverseInv.getInviterId(),
                                conversation.getId(),
                                toResponse(reverseInv, accessToken));
                        notificationService.notifyDirectInvitationAccepted(
                                existingInvitation.getInviterId(),
                                conversation.getId(),
                                toResponse(existingInvitation, accessToken));
                        
                        invitation = existingInvitation;
                    } else {
                        // No reverse invitation or already responded
                        // Return existing invitation to maintain consistency
                        log.warn("Invitation already accepted but conversation status is {}. Invitation ID: {}. Returning existing invitation.", 
                                conversation.getStatus(), existingInvitation.getId());
                        invitation = existingInvitation;
                    }
                }
            } else {
                // DECLINED or EXPIRED - update to PENDING
                log.info("Invitation exists with status {}, updating to PENDING: {}", 
                        existingInvitation.getStatus(), existingInvitation.getId());
                existingInvitation.setStatus("PENDING");
                existingInvitation.setInitialMessage(request.getInitialMessage());
                existingInvitation.setExpiresAt(OffsetDateTime.now().plusDays(7));
                existingInvitation.setRespondedAt(null);
                invitation = invitationRepository.save(existingInvitation);
                log.info("Updated invitation from {} to PENDING. ID: {}", existingInvitation.getStatus(), invitation.getId());
            }
        } else {
            // Create new invitation
            // Check if reverse invitation exists and is PENDING - if so, both users want to chat
            if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                log.info("Reverse invitation exists and is PENDING. Both users want to chat. Auto-accepting both invitations.");
                
                // Auto-accept reverse invitation
                DirectInvitation reverseInv = reverseInvitation.get();
                reverseInv.setStatus("ACCEPTED");
                reverseInv.setRespondedAt(OffsetDateTime.now());
                invitationRepository.save(reverseInv);
                log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                
                // Activate conversation
                conversation.setStatus("ACTIVE");
                conversation = conversationRepository.save(conversation);
                log.info("Activated conversation ID: {}", conversation.getId());
                
                // Create invitation and immediately accept it
                invitation = DirectInvitation.builder()
                        .conversation(conversation)
                        .conversationId(conversation.getId())
                        .inviterId(inviterResidentId)
                        .inviteeId(inviteeResidentId)
                        .status("ACCEPTED")
                        .initialMessage(request.getInitialMessage())
                        .expiresAt(OffsetDateTime.now().plusDays(7))
                        .respondedAt(OffsetDateTime.now())
                        .build();
                invitation = invitationRepository.save(invitation);
                
                log.info("Created and auto-accepted invitation ID: {}, Inviter: {}, Invitee: {}", 
                        invitation.getId(), invitation.getInviterId(), invitation.getInviteeId());
                
                // Create or activate friendship between both users
                friendshipService.createOrActivateFriendship(inviterResidentId, inviteeResidentId);
                log.info("Friendship created/activated between {} and {} (mutual invitations)", inviterResidentId, inviteeResidentId);
                
                // Notify both users that conversation is now active
                notificationService.notifyDirectInvitationAccepted(
                        reverseInv.getInviterId(),
                        conversation.getId(),
                        toResponse(reverseInv, accessToken));
                notificationService.notifyDirectInvitationAccepted(
                        invitation.getInviterId(),
                        conversation.getId(),
                        toResponse(invitation, accessToken));
            } else {
                // Create new invitation normally
                invitation = DirectInvitation.builder()
                        .conversation(conversation)
                        .conversationId(conversation.getId())
                        .inviterId(inviterResidentId)
                        .inviteeId(inviteeResidentId)
                        .status("PENDING")
                        .initialMessage(request.getInitialMessage())
                        .expiresAt(OffsetDateTime.now().plusDays(7))
                        .build();
                invitation = invitationRepository.save(invitation);
                
                log.info("Created new invitation ID: {}, Inviter: {}, Invitee: {}", 
                        invitation.getId(), invitation.getInviterId(), invitation.getInviteeId());
            }
        }
        
        log.info("Created invitation ID: {}, Inviter: {}, Invitee: {}", 
                invitation.getId(), invitation.getInviterId(), invitation.getInviteeId());

        // If initial message provided, create it as a message
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            DirectMessage initialMessage = DirectMessage.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .senderId(inviterResidentId)
                    .content(request.getInitialMessage())
                    .messageType("TEXT")
                    .build();
            messageRepository.save(initialMessage);
        }

        // Notify invitee via WebSocket and FCM
        notificationService.notifyDirectInvitation(inviteeResidentId, toResponse(invitation, accessToken));
        fcmPushService.sendDirectInvitationNotification(inviteeResidentId, inviterResidentId, conversation.getId());

        return toResponse(invitation, accessToken);
    }

    /**
     * Accept invitation
     */
    @Transactional
    public DirectInvitationResponse acceptInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        DirectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify invitee
        if (!invitation.getInviteeId().equals(residentId)) {
            throw new RuntimeException("This invitation is not for you");
        }

        // Check if expired
        if (invitation.isExpired()) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new RuntimeException("Invitation has expired");
        }

        // Check if already responded
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation has already been responded to");
        }

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(OffsetDateTime.now());
        invitation = invitationRepository.save(invitation);

        // Activate conversation
        Conversation conversation = invitation.getConversation();
        conversation.setStatus("ACTIVE");
        conversationRepository.save(conversation);

        // Check if there's a reverse invitation (invitee invited inviter) and auto-accept it
        UUID inviterId = invitation.getInviterId();
        UUID inviteeId = invitation.getInviteeId();
        Optional<DirectInvitation> reverseInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviteeId, inviterId);
        
        if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
            log.info("Found reverse invitation, auto-accepting: {}", reverseInvitation.get().getId());
            DirectInvitation reverseInv = reverseInvitation.get();
            reverseInv.setStatus("ACCEPTED");
            reverseInv.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(reverseInv);
            log.info("Auto-accepted reverse invitation: {}", reverseInv.getId());
        }

        // Create or activate friendship between inviter and invitee
        friendshipService.createOrActivateFriendship(inviterId, inviteeId);
        log.info("Friendship created/activated between {} and {}", inviterId, inviteeId);

        // Notify inviter via WebSocket
        notificationService.notifyDirectInvitationAccepted(
                invitation.getInviterId(),
                conversation.getId(),
                toResponse(invitation, accessToken));

        return toResponse(invitation, accessToken);
    }

    /**
     * Decline invitation
     */
    @Transactional
    public void declineInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        DirectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify invitee
        if (!invitation.getInviteeId().equals(residentId)) {
            throw new RuntimeException("This invitation is not for you");
        }

        // Check if already responded
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation has already been responded to");
        }

        // Update invitation
        invitation.setStatus("DECLINED");
        invitation.setRespondedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        // Close conversation
        Conversation conversation = invitation.getConversation();
        conversation.setStatus("CLOSED");
        conversationRepository.save(conversation);

        // Notify inviter via WebSocket
        notificationService.notifyDirectInvitationDeclined(
                invitation.getInviterId(),
                conversation.getId());
    }

    /**
     * Get pending invitations for a user
     */
    public List<DirectInvitationResponse> getPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        
        log.info("=== getPendingInvitations ===");
        log.info("UserId from JWT: {}", userId);
        log.info("ResidentId converted: {}", residentId);
        
        if (residentId == null) {
            log.error("Resident not found for user: {}", userId);
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<DirectInvitation> invitations = invitationRepository
                .findPendingInvitationsByInviteeId(residentId);
        
        log.info("Found {} pending invitations for residentId: {}", invitations.size(), residentId);
        for (DirectInvitation inv : invitations) {
            log.info("  - Invitation ID: {}, Conversation ID: {}, Inviter: {}, Invitee: {}, Status: {}, ExpiresAt: {}", 
                    inv.getId(), inv.getConversationId(), inv.getInviterId(), inv.getInviteeId(), 
                    inv.getStatus(), inv.getExpiresAt());
        }
        
        // Also check all invitations where this user is invitee (for debugging)
        List<DirectInvitation> allInvitationsAsInvitee = invitationRepository.findAll().stream()
                .filter(inv -> inv.getInviteeId().equals(residentId))
                .collect(Collectors.toList());
        log.info("Total invitations where user {} is invitee: {}", residentId, allInvitationsAsInvitee.size());
        for (DirectInvitation inv : allInvitationsAsInvitee) {
            log.info("  - Invitation ID: {}, Conversation ID: {}, Inviter: {}, Invitee: {}, Status: {}, ExpiresAt: {}", 
                    inv.getId(), inv.getConversationId(), inv.getInviterId(), inv.getInviteeId(), 
                    inv.getStatus(), inv.getExpiresAt());
        }

        return invitations.stream()
                .map(inv -> toResponse(inv, accessToken))
                .collect(Collectors.toList());
    }

    /**
     * Count pending invitations for a user
     */
    public Long countPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            return 0L;
        }

        return invitationRepository.countPendingInvitationsByInviteeId(residentId);
    }

    private DirectInvitationResponse toResponse(DirectInvitation invitation, String accessToken) {
        // Get resident names
        String inviterName = residentInfoService.getResidentName(invitation.getInviterId(), accessToken);
        String inviteeName = residentInfoService.getResidentName(invitation.getInviteeId(), accessToken);

        return DirectInvitationResponse.builder()
                .id(invitation.getId())
                .conversationId(invitation.getConversationId())
                .inviterId(invitation.getInviterId())
                .inviterName(inviterName)
                .inviteeId(invitation.getInviteeId())
                .inviteeName(inviteeName)
                .status(invitation.getStatus())
                .initialMessage(invitation.getInitialMessage())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }
}

