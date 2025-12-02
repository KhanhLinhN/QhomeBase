package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Block;
import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
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
    private final ConversationRepository conversationRepository;

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

        // Close any active conversations between them
        conversationRepository.findConversationBetweenParticipants(blockerId, blockedId)
                .ifPresent(conversation -> {
                    if ("ACTIVE".equals(conversation.getStatus())) {
                        conversation.setStatus("BLOCKED");
                        conversationRepository.save(conversation);
                    }
                });

        log.info("User {} blocked user {}", blockerId, blockedId);
    }

    /**
     * Unblock a user
     */
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new RuntimeException("User is not blocked"));

        blockRepository.delete(block);

        log.info("User {} unblocked user {}", blockerId, blockedId);
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

