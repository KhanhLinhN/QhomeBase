package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Block;
import com.QhomeBase.chatservice.repository.BlockRepository;
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

    /**
     * Block a user
     */
    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
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
     */
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new RuntimeException("User is not blocked"));

        blockRepository.delete(block);

        // Conversation status should already be ACTIVE (we don't change it when blocking)
        // No need to reset status here

        // Reactivate friendship if exists
        friendshipService.createOrActivateFriendship(blockerId, blockedId);

        log.info("User {} unblocked user {}. Messages sent during block will now be visible. Friendship reactivated.", blockerId, blockedId);
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

