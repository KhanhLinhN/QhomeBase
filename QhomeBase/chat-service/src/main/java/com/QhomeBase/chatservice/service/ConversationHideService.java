package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.ConversationParticipant;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationHideService {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ResidentInfoService residentInfoService;

    /**
     * Hide a direct conversation (client-side only)
     * This will reset unreadCount to 0 and mark conversation as hidden
     */
    @Transactional
    public boolean hideDirectConversation(UUID conversationId, UUID userId) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a participant of this conversation"));

            // Mark as hidden
            participant.setIsHidden(true);
            participant.setHiddenAt(OffsetDateTime.now());
            
            // Reset unreadCount by setting lastReadAt to now
            participant.setLastReadAt(OffsetDateTime.now());
            
            conversationParticipantRepository.save(participant);

            log.info("Conversation {} hidden for resident {}", conversationId, residentId);
            return true;
        } catch (Exception e) {
            log.error("Error hiding direct conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Error hiding direct conversation: " + e.getMessage());
        }
    }

    /**
     * Unhide a direct conversation (when new message arrives)
     * This is called automatically when a new message is sent to a hidden conversation
     */
    @Transactional
    public void unhideDirectConversation(UUID conversationId, UUID residentId) {
        try {
            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElse(null);
            
            if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
                participant.setIsHidden(false);
                participant.setHiddenAt(null);
                conversationParticipantRepository.save(participant);
                log.info("Conversation {} unhidden for resident {} (new message received)", conversationId, residentId);
            }
        } catch (Exception e) {
            log.error("Error unhiding direct conversation: {}", e.getMessage(), e);
        }
    }

    private String getCurrentAccessToken() {
        try {
            org.springframework.security.core.Authentication authentication = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.QhomeBase.chatservice.security.UserPrincipal) {
                com.QhomeBase.chatservice.security.UserPrincipal principal = 
                        (com.QhomeBase.chatservice.security.UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

