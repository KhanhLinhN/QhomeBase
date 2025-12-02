package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.DirectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectInvitationRepository extends JpaRepository<DirectInvitation, UUID> {

    /**
     * Find pending invitation for a specific conversation
     */
    Optional<DirectInvitation> findByConversationIdAndStatus(
        UUID conversationId,
        String status
    );

    /**
     * Find pending invitations for a user (as invitee)
     */
    @Query("SELECT i FROM DirectInvitation i WHERE i.inviteeId = :userId " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY i.createdAt DESC")
    List<DirectInvitation> findPendingInvitationsByInviteeId(@Param("userId") UUID userId);

    /**
     * Count pending invitations for a user
     */
    @Query("SELECT COUNT(i) FROM DirectInvitation i WHERE i.inviteeId = :userId " +
           "AND i.status = 'PENDING' " +
           "AND i.expiresAt > CURRENT_TIMESTAMP")
    Long countPendingInvitationsByInviteeId(@Param("userId") UUID userId);

    /**
     * Find invitation by conversation and participants
     */
    @Query("SELECT i FROM DirectInvitation i WHERE i.conversationId = :conversationId " +
           "AND i.inviterId = :inviterId AND i.inviteeId = :inviteeId")
    Optional<DirectInvitation> findByConversationAndParticipants(
        @Param("conversationId") UUID conversationId,
        @Param("inviterId") UUID inviterId,
        @Param("inviteeId") UUID inviteeId
    );
}

