package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;
    private final MessageService messageService;
    private final ChatNotificationService notificationService;

    @Value("${chat.group.max-members:30}")
    private Integer maxMembers;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, UUID userId) {
        // Get residentId from userId
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Create group
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(residentId)
                .buildingId(request.getBuildingId())
                .maxMembers(maxMembers)
                .isActive(true)
                .build();

        group = groupRepository.save(group);

        // Add creator as ADMIN
        GroupMember creatorMember = GroupMember.builder()
                .group(group)
                .groupId(group.getId())
                .residentId(residentId)
                .role("ADMIN")
                .isMuted(false)
                .build();
        groupMemberRepository.save(creatorMember);

        // Add other members if provided
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            long currentCount = groupMemberRepository.countByGroupId(group.getId());
            int remainingSlots = maxMembers - (int) currentCount;
            
            List<UUID> membersToAdd = request.getMemberIds().stream()
                    .limit(remainingSlots)
                    .collect(Collectors.toList());

            for (UUID memberId : membersToAdd) {
                if (!memberId.equals(residentId) && 
                    !groupMemberRepository.existsByGroupIdAndResidentId(group.getId(), memberId)) {
                    GroupMember member = GroupMember.builder()
                            .group(group)
                            .groupId(group.getId())
                            .residentId(memberId)
                            .role("MEMBER")
                            .isMuted(false)
                            .build();
                    groupMemberRepository.save(member);
                }
            }
        }

        return toGroupResponse(group, residentId);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId, UUID userId) {
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

        return toGroupResponse(group, residentId);
    }

    @Transactional(readOnly = true)
    public GroupPagedResponse getGroupsByResident(UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Group> groups = groupRepository.findGroupsByResidentId(residentId, pageable);

        Page<GroupResponse> responsePage = groups.map(group -> toGroupResponse(group, residentId));

        return GroupPagedResponse.builder()
                .content(responsePage.getContent())
                .currentPage(responsePage.getNumber())
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .isFirst(responsePage.isFirst())
                .isLast(responsePage.isLast())
                .build();
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member of the group
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // All members can update group name
        // Only admins can update description and avatar
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            if (!"ADMIN".equals(member.getRole())) {
                throw new RuntimeException("Only admins can update group description");
            }
            group.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            if (!"ADMIN".equals(member.getRole())) {
                throw new RuntimeException("Only admins can update group avatar");
            }
            group.setAvatarUrl(request.getAvatarUrl());
        }

        group = groupRepository.save(group);
        GroupResponse response = toGroupResponse(group, residentId);
        
        // Notify via WebSocket
        notificationService.notifyGroupUpdated(group.getId(), response);
        
        return response;
    }

    @Transactional
    public void addMembers(UUID groupId, AddMembersRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or MODERATOR
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!"ADMIN".equals(member.getRole()) && !"MODERATOR".equals(member.getRole())) {
            throw new RuntimeException("Only admins and moderators can add members");
        }

        long currentCount = groupMemberRepository.countByGroupId(groupId);
        int remainingSlots = group.getMaxMembers() - (int) currentCount;

        if (remainingSlots <= 0) {
            throw new RuntimeException("Group is full. Maximum " + group.getMaxMembers() + " members allowed.");
        }

        List<UUID> membersToAdd = request.getMemberIds().stream()
                .limit(remainingSlots)
                .filter(memberId -> !groupMemberRepository.existsByGroupIdAndResidentId(groupId, memberId))
                .collect(Collectors.toList());

        for (UUID memberId : membersToAdd) {
            GroupMember newMember = GroupMember.builder()
                    .group(group)
                    .groupId(group.getId())
                    .residentId(memberId)
                    .role("MEMBER")
                    .isMuted(false)
                    .build();
            groupMemberRepository.save(newMember);
            
            // Notify via WebSocket
            GroupMemberResponse memberResponse = toGroupMemberResponse(newMember);
            notificationService.notifyMemberAdded(groupId, memberResponse);
        }
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or removing themselves
        GroupMember requester = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!residentId.equals(memberId) && !"ADMIN".equals(requester.getRole())) {
            throw new RuntimeException("Only admins can remove other members");
        }

        GroupMember memberToRemove = groupMemberRepository.findByGroupIdAndResidentId(groupId, memberId)
                .orElseThrow(() -> new RuntimeException("Member not found in group"));

        if ("ADMIN".equals(memberToRemove.getRole())) {
            // Check if there's at least one other admin
            List<GroupMember> admins = groupMemberRepository.findAdminsByGroupId(groupId);
            if (admins.size() <= 1) {
                throw new RuntimeException("Cannot remove the last admin");
            }
        }

        UUID removedMemberId = memberToRemove.getResidentId();
        
        // Get member name before deleting
        Map<String, Object> memberInfo = residentInfoService.getResidentInfo(removedMemberId);
        String memberName = memberInfo != null ? (String) memberInfo.get("fullName") : "Một thành viên";
        
        groupMemberRepository.delete(memberToRemove);
        
        // Create system message
        String systemMessageContent = memberName + " đã rời khỏi nhóm";
        try {
            messageService.createSystemMessage(groupId, systemMessageContent);
        } catch (Exception e) {
            log.warn("Failed to create system message for member leave: {}", e.getMessage());
        }
        
        // Notify via WebSocket
        notificationService.notifyMemberRemoved(groupId, removedMemberId);
    }

    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }
        removeMember(groupId, residentId, userId);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Only the creator (owner) can delete the group
        if (!group.getCreatedBy().equals(residentId)) {
            throw new RuntimeException("Only the group creator can delete the group");
        }

        // Soft delete: mark group as inactive
        group.setIsActive(false);
        groupRepository.save(group);

        // Notify via WebSocket
        notificationService.notifyGroupDeleted(groupId);
    }

    private GroupResponse toGroupResponse(Group group, UUID currentResidentId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        
        String userRole = null;
        Long unreadCount = 0L;
        GroupMember currentMember = groupMemberRepository.findByGroupIdAndResidentId(group.getId(), currentResidentId).orElse(null);
        if (currentMember != null) {
            userRole = currentMember.getRole();
            // Calculate unread count (messages after lastReadAt, excluding own messages)
            unreadCount = messageService.countUnreadMessages(
                group.getId(), 
                currentMember.getLastReadAt(), 
                currentResidentId // Exclude messages sent by current user
            );
        }

        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::toGroupMemberResponse)
                .collect(Collectors.toList());

        // Get creator info
        Map<String, Object> creatorInfo = residentInfoService.getResidentInfo(group.getCreatedBy());
        String createdByName = creatorInfo != null ? (String) creatorInfo.get("fullName") : null;

        // Get building name if buildingId exists
        String buildingName = null;
        if (group.getBuildingId() != null) {
            // TODO: Call base-service to get building name
        }

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdByName(createdByName)
                .buildingId(group.getBuildingId())
                .buildingName(buildingName)
                .avatarUrl(group.getAvatarUrl())
                .maxMembers(group.getMaxMembers())
                .currentMemberCount(members.size())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .members(memberResponses)
                .userRole(userRole)
                .unreadCount(unreadCount)
                .build();
    }

    private GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(member.getResidentId());
        String residentName = residentInfo != null ? (String) residentInfo.get("fullName") : null;
        String residentAvatar = null; // TODO: Get avatar from resident info

        return GroupMemberResponse.builder()
                .id(member.getId())
                .groupId(member.getGroupId())
                .residentId(member.getResidentId())
                .residentName(residentName)
                .residentAvatar(residentAvatar)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .lastReadAt(member.getLastReadAt())
                .isMuted(member.getIsMuted())
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

