package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.GroupInvitationResponse;
import com.QhomeBase.chatservice.dto.InviteMembersByPhoneRequest;
import com.QhomeBase.chatservice.dto.InviteMembersResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupInvitation;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.repository.GroupInvitationRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupInvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;

    @Value("${base.service.url:http://localhost:8081}")
    private String baseServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * Find resident by phone number from base-service
     */
    private UUID findResidentIdByPhone(String phone, String accessToken) {
        try {
            String url = baseServiceUrl + "/api/residents/by-phone/" + phone;
            
            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                return null;
            }
            
            Object idObj = response.get("id");
            if (idObj == null) {
                return null;
            }
            
            return UUID.fromString(idObj.toString());
        } catch (Exception e) {
            log.debug("Resident not found for phone: {}", phone);
            return null;
        }
    }

    /**
     * Invite members to group by phone numbers
     */
    @Transactional
    public InviteMembersResponse inviteMembersByPhone(UUID groupId, InviteMembersByPhoneRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID inviterResidentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (inviterResidentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or MODERATOR
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, inviterResidentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!"ADMIN".equals(member.getRole()) && !"MODERATOR".equals(member.getRole())) {
            throw new RuntimeException("Only admins and moderators can invite members");
        }

        // Check group capacity
        long currentCount = groupMemberRepository.countByGroupId(groupId);
        int remainingSlots = group.getMaxMembers() - (int) currentCount;

        if (remainingSlots <= 0) {
            throw new RuntimeException("Group is full. Maximum " + group.getMaxMembers() + " members allowed.");
        }

        List<GroupInvitationResponse> successfulInvitations = new ArrayList<>();
        List<String> invalidPhones = new ArrayList<>();
        List<String> skippedPhones = new ArrayList<>();
        
        Map<String, Object> inviterInfo = residentInfoService.getResidentInfo(inviterResidentId);
        String inviterName = inviterInfo != null ? (String) inviterInfo.get("fullName") : null;

        for (String phone : request.getPhoneNumbers()) {
            if (successfulInvitations.size() >= remainingSlots) {
                skippedPhones.add(phone + " (Nhóm đã đầy)");
                continue; // Group is full
            }

            // Normalize phone number (remove spaces, dashes, etc.)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            // Validate phone number format (should be 9-10 digits after normalization)
            if (normalizedPhone.length() < 9 || normalizedPhone.length() > 11) {
                invalidPhones.add(phone + " (Định dạng không hợp lệ)");
                continue;
            }
            
            // Ensure consistent format: remove leading zero if present for storage
            // Vietnamese phones: 0123456789 -> 123456789 for consistency
            String phoneForStorage = normalizedPhone;
            if (normalizedPhone.startsWith("0") && normalizedPhone.length() > 1) {
                phoneForStorage = normalizedPhone.substring(1);
            }
            
            log.debug("Normalized phone from '{}' to '{}' (storage: '{}')", phone, normalizedPhone, phoneForStorage);

            // Validate: Check if phone number exists in database
            UUID residentId = findResidentIdByPhone(phoneForStorage, accessToken);
            if (residentId == null && !phoneForStorage.startsWith("0")) {
                // Try with leading zero
                residentId = findResidentIdByPhone("0" + phoneForStorage, accessToken);
            }
            
            if (residentId == null) {
                // Phone number doesn't exist in database
                invalidPhones.add(phone + " (Số điện thoại không tồn tại trong hệ thống)");
                log.warn("Phone number '{}' (normalized: '{}') not found in database", phone, phoneForStorage);
                continue;
            }

            // Check if already a member
            if (groupMemberRepository.existsByGroupIdAndResidentId(groupId, residentId)) {
                skippedPhones.add(phone + " (Đã là thành viên)");
                log.info("Resident {} is already a member of group {}", residentId, groupId);
                continue;
            }

            // Check if there's already a pending invitation (try both formats)
            Optional<GroupInvitation> existing = invitationRepository.findPendingByGroupIdAndPhone(groupId, phoneForStorage);
            if (existing.isEmpty() && !phoneForStorage.startsWith("0")) {
                // Also try with leading zero
                existing = invitationRepository.findPendingByGroupIdAndPhone(groupId, "0" + phoneForStorage);
            }
            if (existing.isPresent()) {
                skippedPhones.add(phone + " (Đã có lời mời đang chờ)");
                log.info("Pending invitation already exists for phone {} in group {}", phoneForStorage, groupId);
                continue;
            }

            // Create invitation
            GroupInvitation invitation = GroupInvitation.builder()
                    .group(group)
                    .groupId(groupId)
                    .inviterId(inviterResidentId)
                    .inviteePhone(phoneForStorage)
                    .inviteeResidentId(residentId)
                    .status("PENDING")
                    .expiresAt(OffsetDateTime.now().plusDays(7))
                    .build();

            invitation = invitationRepository.save(invitation);

            // Send notification
            String title = "Lời mời tham gia nhóm";
            String body = inviterName != null 
                ? inviterName + " mời bạn tham gia nhóm \"" + group.getName() + "\""
                : "Bạn được mời tham gia nhóm \"" + group.getName() + "\"";

            Map<String, String> data = new java.util.HashMap<>();
            data.put("type", "GROUP_INVITATION");
            data.put("groupId", groupId.toString());
            data.put("groupName", group.getName());
            data.put("invitationId", invitation.getId().toString());
            data.put("inviterId", inviterResidentId.toString());
            data.put("inviterName", inviterName != null ? inviterName : "");

            // Send FCM push notification
            fcmPushService.sendPushToResident(residentId, title, body, data);

            // Send WebSocket notification
            // TODO: Add WebSocket notification for invitation

            GroupInvitationResponse response = GroupInvitationResponse.builder()
                    .id(invitation.getId())
                    .groupId(groupId)
                    .groupName(group.getName())
                    .inviterId(inviterResidentId)
                    .inviterName(inviterName)
                    .inviteePhone(phoneForStorage)
                    .inviteeResidentId(residentId)
                    .status(invitation.getStatus())
                    .createdAt(invitation.getCreatedAt())
                    .expiresAt(invitation.getExpiresAt())
                    .build();

            successfulInvitations.add(response);
            log.info("Successfully created invitation for phone {} (residentId: {})", phone, residentId);
        }

        return InviteMembersResponse.builder()
                .successfulInvitations(successfulInvitations)
                .invalidPhones(invalidPhones)
                .skippedPhones(skippedPhones)
                .build();
    }

    /**
     * Get pending invitations for current user
     */
    @Transactional(readOnly = true)
    public List<GroupInvitationResponse> getMyPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<GroupInvitation> invitations = new ArrayList<>();

        // First, try to find by residentId (if invitation was created after resident was found)
        List<GroupInvitation> byResidentId = invitationRepository.findPendingInvitationsByResidentId(residentId);
        invitations.addAll(byResidentId);
        log.debug("Found {} invitations by residentId: {}", byResidentId.size(), residentId);

        // Also try to find by phone number (for invitations created before resident was found)
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
        String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
        
        if (phone != null && !phone.isEmpty()) {
            // Normalize phone number (remove all non-digit characters)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            log.debug("Looking for invitations with phone: {} (normalized: {})", phone, normalizedPhone);
            
            // Try exact match
            List<GroupInvitation> byPhone = invitationRepository.findPendingInvitationsByPhone(normalizedPhone);
            
            // Also try with leading zero (Vietnamese phone format: 0123456789)
            String withLeadingZero = normalizedPhone.startsWith("0") ? normalizedPhone : "0" + normalizedPhone;
            List<GroupInvitation> byPhoneWithZero = invitationRepository.findPendingInvitationsByPhone(withLeadingZero);
            
            // Also try without leading zero (if phone starts with 0)
            String withoutLeadingZero = normalizedPhone.startsWith("0") && normalizedPhone.length() > 1 
                ? normalizedPhone.substring(1) 
                : normalizedPhone;
            List<GroupInvitation> byPhoneWithoutZero = invitationRepository.findPendingInvitationsByPhone(withoutLeadingZero);
            
            // Merge results, avoiding duplicates
            for (GroupInvitation inv : byPhone) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                }
            }
            for (GroupInvitation inv : byPhoneWithZero) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                }
            }
            for (GroupInvitation inv : byPhoneWithoutZero) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                }
            }
            
            log.debug("Found {} invitations by phone (total unique: {})", 
                byPhone.size() + byPhoneWithZero.size() + byPhoneWithoutZero.size(), invitations.size());
        } else {
            log.warn("Phone number not found for residentId: {}", residentId);
        }

        log.info("Total pending invitations found for residentId {}: {}", residentId, invitations.size());

        return invitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Accept invitation
     */
    @Transactional
    public void acceptInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        GroupInvitation invitation = invitationRepository.findByIdAndStatus(invitationId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Invitation not found or already processed"));

        // Verify phone number matches
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
        String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
        if (phone == null) {
            throw new RuntimeException("Phone number not found for resident");
        }

        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        if (!normalizedPhone.equals(invitation.getInviteePhone())) {
            throw new RuntimeException("This invitation is not for you");
        }

        // Check if already a member
        if (groupMemberRepository.existsByGroupIdAndResidentId(invitation.getGroupId(), residentId)) {
            invitation.setStatus("ACCEPTED");
            invitation.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(invitation);
            return; // Already a member
        }

        // Check group capacity
        Group group = groupRepository.findActiveGroupById(invitation.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        long currentCount = groupMemberRepository.countByGroupId(invitation.getGroupId());
        if (currentCount >= group.getMaxMembers()) {
            throw new RuntimeException("Group is full");
        }

        // Add as member
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .groupId(invitation.getGroupId())
                .residentId(residentId)
                .role("MEMBER")
                .isMuted(false)
                .build();
        groupMemberRepository.save(newMember);

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setInviteeResidentId(residentId);
        invitation.setRespondedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        // Notify via WebSocket
        com.QhomeBase.chatservice.dto.GroupMemberResponse memberResponse = toGroupMemberResponse(newMember);
        notificationService.notifyMemberAdded(invitation.getGroupId(), memberResponse);
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

        GroupInvitation invitation = invitationRepository.findByIdAndStatus(invitationId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Invitation not found or already processed"));

        // Verify phone number matches
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
        String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
        if (phone == null) {
            throw new RuntimeException("Phone number not found for resident");
        }

        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        if (!normalizedPhone.equals(invitation.getInviteePhone())) {
            throw new RuntimeException("This invitation is not for you");
        }

        invitation.setStatus("DECLINED");
        invitation.setInviteeResidentId(residentId);
        invitation.setRespondedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    private GroupInvitationResponse toInvitationResponse(GroupInvitation invitation) {
        Map<String, Object> inviterInfo = residentInfoService.getResidentInfo(invitation.getInviterId());
        String inviterName = inviterInfo != null ? (String) inviterInfo.get("fullName") : null;

        Group group = groupRepository.findById(invitation.getGroupId()).orElse(null);
        String groupName = group != null ? group.getName() : null;

        return GroupInvitationResponse.builder()
                .id(invitation.getId())
                .groupId(invitation.getGroupId())
                .groupName(groupName)
                .inviterId(invitation.getInviterId())
                .inviterName(inviterName)
                .inviteePhone(invitation.getInviteePhone())
                .inviteeResidentId(invitation.getInviteeResidentId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private com.QhomeBase.chatservice.dto.GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(member.getResidentId());
        String residentName = residentInfo != null ? (String) residentInfo.get("fullName") : null;
        String residentAvatar = null; // TODO: Get avatar from resident info

        return com.QhomeBase.chatservice.dto.GroupMemberResponse.builder()
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

