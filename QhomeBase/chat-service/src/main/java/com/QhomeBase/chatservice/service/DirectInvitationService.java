package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.CreateDirectInvitationRequest;
import com.QhomeBase.chatservice.dto.DirectInvitationResponse;
import com.QhomeBase.chatservice.model.Block;
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

        UUID inviteeResidentId = request.getInviteeId();

        // Check if users are blocked
        if (blockRepository.findByBlockerIdAndBlockedId(inviterResidentId, inviteeResidentId).isPresent() ||
            blockRepository.findByBlockerIdAndBlockedId(inviteeResidentId, inviterResidentId).isPresent()) {
            throw new RuntimeException("Cannot send invitation: User is blocked");
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
            
            // If conversation exists but is PENDING, check if invitation exists
            if ("PENDING".equals(conversation.getStatus())) {
                DirectInvitation existingInvitation = invitationRepository
                        .findByConversationIdAndStatus(conversation.getId(), "PENDING")
                        .orElse(null);
                
                if (existingInvitation != null && !existingInvitation.isExpired()) {
                    throw new RuntimeException("Invitation already exists and is pending");
                }
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

        // Create invitation
        DirectInvitation invitation = DirectInvitation.builder()
                .conversation(conversation)
                .conversationId(conversation.getId())
                .inviterId(inviterResidentId)
                .inviteeId(inviteeResidentId)
                .status("PENDING")
                .initialMessage(request.getInitialMessage())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        invitation = invitationRepository.save(invitation);

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
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<DirectInvitation> invitations = invitationRepository
                .findPendingInvitationsByInviteeId(residentId);

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

