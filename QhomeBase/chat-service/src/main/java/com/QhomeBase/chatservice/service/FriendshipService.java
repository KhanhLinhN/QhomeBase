package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Friendship;
import com.QhomeBase.chatservice.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    /**
     * Create or activate friendship between two users
     */
    @Transactional
    public Friendship createOrActivateFriendship(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.warn("Cannot create/activate friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            throw new RuntimeException("User IDs cannot be null");
        }
        
        // Ensure user1_id < user2_id for consistency
        UUID user1Id = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID user2Id = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .map(existingFriendship -> {
                    // Friendship exists - activate it if inactive
                    if (!Boolean.TRUE.equals(existingFriendship.getIsActive())) {
                        existingFriendship.setIsActive(true);
                        existingFriendship = friendshipRepository.save(existingFriendship);
                        log.info("Activated existing friendship between {} and {}", user1Id, user2Id);
                    }
                    return existingFriendship;
                })
                .orElseGet(() -> {
                    // Create new friendship
                    Friendship friendship = Friendship.builder()
                            .user1Id(user1Id)
                            .user2Id(user2Id)
                            .isActive(true)
                            .build();
                    friendship = friendshipRepository.save(friendship);
                    log.info("Created new friendship between {} and {}", user1Id, user2Id);
                    return friendship;
                });
    }

    /**
     * Deactivate friendship (when one user blocks the other)
     */
    @Transactional
    public void deactivateFriendship(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.warn("Cannot deactivate friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            return;
        }
        
        UUID user1Id = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID user2Id = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .ifPresent(friendship -> {
                    if (Boolean.TRUE.equals(friendship.getIsActive())) {
                        friendship.setIsActive(false);
                        friendshipRepository.save(friendship);
                        log.info("Deactivated friendship between {} and {} (user blocked)", user1Id, user2Id);
                    }
                });
    }

    /**
     * Get all active friendships for a user
     */
    @Transactional(readOnly = true)
    public List<Friendship> getActiveFriendships(UUID userId) {
        return friendshipRepository.findActiveFriendshipsByUserId(userId);
    }

    /**
     * Check if two users are friends (active friendship exists)
     */
    @Transactional(readOnly = true)
    public boolean areFriends(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.debug("Cannot check friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            return false;
        }
        
        UUID user1Id = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID user2Id = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .map(friendship -> Boolean.TRUE.equals(friendship.getIsActive()))
                .orElse(false);
    }
}

