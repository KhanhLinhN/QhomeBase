package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.GroupFileResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupFile;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.model.Message;
import com.QhomeBase.chatservice.repository.GroupFileRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.QhomeBase.chatservice.dto.GroupFilePagedResponse;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupFileService {

    private final GroupFileRepository groupFileRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;

    /**
     * Get all files in a group with pagination
     */
    @Transactional(readOnly = true)
    public GroupFilePagedResponse getGroupFiles(UUID groupId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupFile> files = groupFileRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Convert to GroupFilePagedResponse
        return GroupFilePagedResponse.builder()
                .content(files.getContent().stream()
                        .map(this::toGroupFileResponse)
                        .collect(java.util.stream.Collectors.toList()))
                .currentPage(files.getNumber())
                .pageSize(files.getSize())
                .totalElements(files.getTotalElements())
                .totalPages(files.getTotalPages())
                .hasNext(files.hasNext())
                .hasPrevious(files.hasPrevious())
                .isFirst(files.isFirst())
                .isLast(files.isLast())
                .build();
    }

    /**
     * Save file metadata when a file message is sent
     */
    @Transactional
    public GroupFile saveFileMetadata(Message message) {
        // Only save for FILE, IMAGE, and AUDIO message types
        if (!"FILE".equals(message.getMessageType()) && 
            !"IMAGE".equals(message.getMessageType()) && 
            !"AUDIO".equals(message.getMessageType())) {
            return null;
        }

        // Check if file metadata already exists for this message
        if (groupFileRepository.existsByMessageId(message.getId())) {
            return groupFileRepository.findByMessageId(message.getId());
        }

        // Determine file URL based on message type
        String fileUrl = null;
        if ("IMAGE".equals(message.getMessageType())) {
            fileUrl = message.getImageUrl();
        } else if ("FILE".equals(message.getMessageType()) || "AUDIO".equals(message.getMessageType())) {
            fileUrl = message.getFileUrl();
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        GroupFile groupFile = GroupFile.builder()
                .group(message.getGroup())
                .groupId(message.getGroupId())
                .message(message)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .fileName(message.getFileName() != null ? message.getFileName() : "file")
                .fileSize(message.getFileSize() != null ? message.getFileSize() : 0L)
                .fileType(message.getMimeType())
                .fileUrl(fileUrl)
                .build();

        return groupFileRepository.save(groupFile);
    }

    /**
     * Convert GroupFile entity to GroupFileResponse DTO
     */
    private GroupFileResponse toGroupFileResponse(GroupFile groupFile) {
        String accessToken = getCurrentAccessToken();
        
        // Get sender info
        Map<String, Object> senderInfo = residentInfoService.getResidentInfo(groupFile.getSenderId());
        String senderName = senderInfo != null ? (String) senderInfo.get("fullName") : "Người dùng";
        String senderAvatarUrl = senderInfo != null ? (String) senderInfo.get("avatarUrl") : null;

        return GroupFileResponse.builder()
                .id(groupFile.getId())
                .groupId(groupFile.getGroupId())
                .messageId(groupFile.getMessageId())
                .senderId(groupFile.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatarUrl)
                .fileName(groupFile.getFileName())
                .fileSize(groupFile.getFileSize())
                .fileType(groupFile.getFileType())
                .fileUrl(groupFile.getFileUrl())
                .createdAt(groupFile.getCreatedAt())
                .build();
    }

    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

