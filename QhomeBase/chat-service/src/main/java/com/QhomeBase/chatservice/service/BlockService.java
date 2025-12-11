package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Block;
import com.QhomeBase.chatservice.model.DirectInvitation;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.DirectInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {

    private final BlockRepository blockRepository;
    private final FriendshipService friendshipService;
    private final DirectInvitationRepository invitationRepository;

    /**
     * Block a user
     */
    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new RuntimeException("Blocker ID and blocked ID cannot be null");
        }
        
        if (blockerId.equals(blockedId)) {
            throw new RuntimeException("Cannot block yourself");
        }

        // Check if already blocked
        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).isPresent()) {
            throw new RuntimeException("User is already blocked");
        }

        // Create block record
        Block block = Block.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        blockRepository.save(block);

        // Don't change conversation status - keep it ACTIVE so both users can still see the conversation
        // The blocker can still send messages, but the blocked user won't receive FCM notifications
        // and will see "User not found" message in the chat input area

        // Deactivate friendship if exists
        friendshipService.deactivateFriendship(blockerId, blockedId);

        log.info("User {} blocked user {}. Conversation remains ACTIVE. Friendship deactivated.", blockerId, blockedId);
    }

    /**
     * Unblock a user
     * This method is idempotent - if the user is not blocked, it will return successfully without error
     */
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new RuntimeException("Blocker ID and blocked ID cannot be null");
        }
        
        // Check if user is actually blocked
        var blockOpt = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);
        
        if (blockOpt.isEmpty()) {
            // User is not blocked - this is fine, just return (idempotent operation)
            log.debug("User {} attempted to unblock user {}, but user was not blocked. Operation completed successfully (idempotent).", blockerId, blockedId);
            return;
        }

        Block block = blockOpt.get();
        blockRepository.delete(block);

        // Conversation status should already be ACTIVE (we don't change it when blocking)
        // No need to reset status here

        // Do NOT reactivate friendship - users are no longer friends after block/unblock
        // They need to send invitation again to become friends and chat directly
        // Friendship remains inactive/deleted - users must re-establish friendship through invitation
        
        // Reset ACCEPTED invitations to PENDING so users can see and accept them again
        // Find all invitations between the two users (bidirectional)
        List<DirectInvitation> invitations = invitationRepository.findInvitationsBetweenUsers(blockerId, blockedId);
        
        for (DirectInvitation inv : invitations) {
            if ("ACCEPTED".equals(inv.getStatus())) {
                inv.setStatus("PENDING");
                inv.setRespondedAt(null);
                invitationRepository.save(inv);
                log.info("Reset ACCEPTED invitation {} to PENDING after unblock ({} -> {})", 
                        inv.getId(), inv.getInviterId(), inv.getInviteeId());
            }
        }

        log.info("User {} unblocked user {}. Friendship remains inactive - invitations reset to PENDING so users can accept again.", blockerId, blockedId);
    }

    /**
     * Check if user1 has blocked user2
     */
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        return blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).isPresent();
    }

    /**
     * Check if two users have blocked each other (bidirectional)
     */
    public boolean areBlocked(UUID userId1, UUID userId2) {
        List<Block> blocks = blockRepository.findBlocksBetweenUsers(userId1, userId2);
        return !blocks.isEmpty();
    }

    /**
     * Get all blocked users for a user
     */
    public List<UUID> getBlockedUserIds(UUID userId) {
        return blockRepository.findByBlockerId(userId).stream()
                .map(Block::getBlockedId)
                .toList();
    }
}

