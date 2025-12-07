package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.ConversationParticipant;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationHideService {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
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
            
            // Reset lastReadAt to null so that when a new message arrives,
            // it will be considered as the first message (like Messenger)
            participant.setLastReadAt(null);
            
            conversationParticipantRepository.save(participant);

            // Check if both participants have hidden the conversation
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElse(null);
            if (conversation != null) {
                List<ConversationParticipant> allParticipants = conversationParticipantRepository
                        .findByConversationId(conversationId);
                
                boolean bothHidden = allParticipants.stream()
                        .allMatch(p -> Boolean.TRUE.equals(p.getIsHidden()));
                
                if (bothHidden && allParticipants.size() == 2) {
                    // Both participants have hidden the conversation - mark as DELETED
                    conversation.setStatus("DELETED");
                    conversationRepository.save(conversation);
                    log.info("Conversation {} marked as DELETED (both participants have hidden it)", conversationId);
                }
            }

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
                // Reset lastReadAt to null so the new message is considered as the first message
                participant.setLastReadAt(null);
                conversationParticipantRepository.save(participant);
                log.info("Conversation {} unhidden for resident {} (new message received). lastReadAt reset to null.", conversationId, residentId);
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

