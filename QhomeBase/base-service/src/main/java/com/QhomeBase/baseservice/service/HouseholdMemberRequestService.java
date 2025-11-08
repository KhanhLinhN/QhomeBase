package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.HouseholdMemberCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDecisionDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMemberRequest;
import com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdMemberRequestRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
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
public class HouseholdMemberRequestService {

    private final HouseholdMemberRequestRepository requestRepository;
    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseHoldMemberService houseHoldMemberService;
    private final UnitRepository unitRepository;

    @Transactional
    public HouseholdMemberRequestDto createRequest(HouseholdMemberRequestCreateDto createDto, UserPrincipal principal) {
        Household household = householdRepository.findById(createDto.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        Resident requesterResident = residentRepository.findByUserId(principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Requester is not linked to any resident"));

        if (!isPrimaryResident(household, requesterResident.getId())) {
            throw new IllegalArgumentException("Only the primary resident can submit membership requests");
        }

        UUID resolvedResidentId = resolveExistingResident(createDto);

        if (resolvedResidentId != null) {
            requestRepository.findFirstByHouseholdIdAndResidentIdAndStatusIn(
                    createDto.householdId(),
                    resolvedResidentId,
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("There is already a pending request for this resident");
            });

            householdMemberRepository.findActiveMemberByResidentAndHousehold(resolvedResidentId, createDto.householdId())
                    .ifPresent(member -> {
                        throw new IllegalArgumentException("Resident is already a member of this household");
                    });
        } else if (createDto.residentPhone() != null && !createDto.residentPhone().isBlank()) {
            requestRepository.findFirstByHouseholdIdAndResidentFullNameIgnoreCaseAndResidentPhoneAndStatusIn(
                    createDto.householdId(),
                    createDto.residentFullName(),
                    createDto.residentPhone(),
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("There is already a pending request for this resident");
            });
        }

        HouseholdMemberRequest request = HouseholdMemberRequest.builder()
                .householdId(createDto.householdId())
                .residentId(resolvedResidentId)
                .requestedBy(principal.uid())
                .residentFullName(createDto.residentFullName())
                .residentPhone(createDto.residentPhone())
                .residentEmail(createDto.residentEmail())
                .residentNationalId(createDto.residentNationalId())
                .residentDob(createDto.residentDob())
                .relation(createDto.relation())
                .proofOfRelationImageUrl(createDto.proofOfRelationImageUrl())
                .note(createDto.note())
                .status(RequestStatus.PENDING)
                .build();

        HouseholdMemberRequest saved = requestRepository.save(request);
        log.info("Household member request {} created by {}", saved.getId(), principal.uid());

        return toDto(saved);
    }

    @Transactional
    public HouseholdMemberRequestDto decideRequest(UUID requestId, HouseholdMemberRequestDecisionDto decisionDto, UUID adminUserId) {
        HouseholdMemberRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Household member request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Request has already been processed");
        }

        if (Boolean.TRUE.equals(decisionDto.approve())) {
            approveRequest(request, adminUserId);
        } else {
            rejectRequest(request, adminUserId, decisionDto.rejectionReason());
        }

        HouseholdMemberRequest saved = requestRepository.save(request);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberRequestDto> getRequestsForUser(UUID requesterUserId) {
        return requestRepository.findByRequestedByOrderByCreatedAtDesc(requesterUserId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberRequestDto> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void approveRequest(HouseholdMemberRequest request, UUID adminUserId) {
        householdRepository.findById(request.getHouseholdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        UUID residentId = ensureResidentExists(request);

        householdMemberRepository.findActiveMemberByResidentAndHousehold(residentId, request.getHouseholdId())
                .ifPresent(member -> {
                    throw new IllegalArgumentException("Resident is already a member of this household");
                });

        houseHoldMemberService.createHouseholdMember(new HouseholdMemberCreateDto(
                request.getHouseholdId(),
                residentId,
                request.getRelation(),
                request.getProofOfRelationImageUrl(),
                Boolean.FALSE,
                null
        ));

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(adminUserId);
        request.setApprovedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());
        log.info("Household member request {} approved by {}", request.getId(), adminUserId);
    }

    private void rejectRequest(HouseholdMemberRequest request, UUID adminUserId, String rejectionReason) {
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectedBy(adminUserId);
        request.setRejectedAt(OffsetDateTime.now());
        request.setRejectionReason(rejectionReason);
        request.setUpdatedAt(OffsetDateTime.now());
        log.info("Household member request {} rejected by {}", request.getId(), adminUserId);
    }

    private boolean isPrimaryResident(Household household, UUID residentId) {
        if (household.getPrimaryResidentId() != null && household.getPrimaryResidentId().equals(residentId)) {
            return true;
        }

        return householdMemberRepository.findPrimaryMemberByHouseholdId(household.getId())
                .map(member -> residentId.equals(member.getResidentId()))
                .orElse(false);
    }

    private HouseholdMemberRequestDto toDto(HouseholdMemberRequest request) {
        Household household = householdRepository.findById(request.getHouseholdId()).orElse(null);
        UUID unitId = null;
        String unitCode = null;
        String householdCode = null;
        if (household != null) {
            unitId = household.getUnitId();
            householdCode = household.getId().toString();
            if (unitId != null) {
                var unit = unitRepository.findById(unitId).orElse(null);
                if (unit != null) {
                    unitCode = unit.getCode();
                    householdCode = unit.getCode();
                }
            }
        }

        Resident resident = null;
        if (request.getResidentId() != null) {
            resident = residentRepository.findById(request.getResidentId()).orElse(null);
        }
        String residentName = resident != null ? resident.getFullName() : null;
        String residentEmail = resident != null ? resident.getEmail() : null;
        String residentPhone = resident != null ? resident.getPhone() : null;

        Resident requesterResident = residentRepository.findByUserId(request.getRequestedBy()).orElse(null);
        String requestedByName = requesterResident != null ? requesterResident.getFullName() : null;

        String approvedByName = null;
        if (request.getApprovedBy() != null) {
            Resident approved = residentRepository.findByUserId(request.getApprovedBy()).orElse(null);
            approvedByName = approved != null ? approved.getFullName() : null;
        }

        String rejectedByName = null;
        if (request.getRejectedBy() != null) {
            Resident rejected = residentRepository.findByUserId(request.getRejectedBy()).orElse(null);
            rejectedByName = rejected != null ? rejected.getFullName() : null;
        }

        return new HouseholdMemberRequestDto(
                request.getId(),
                request.getHouseholdId(),
                householdCode,
                unitId,
                unitCode,
                request.getResidentId(),
                residentName,
                residentEmail,
                residentPhone,
                request.getResidentFullName(),
                request.getResidentPhone(),
                request.getResidentEmail(),
                request.getResidentNationalId(),
                request.getResidentDob(),
                request.getRequestedBy(),
                requestedByName,
                request.getRelation(),
                request.getProofOfRelationImageUrl(),
                request.getNote(),
                request.getStatus(),
                request.getApprovedBy(),
                approvedByName,
                request.getRejectedBy(),
                rejectedByName,
                request.getRejectionReason(),
                request.getApprovedAt(),
                request.getRejectedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private UUID resolveExistingResident(HouseholdMemberRequestCreateDto createDto) {
        if (createDto.residentNationalId() != null && !createDto.residentNationalId().isBlank()) {
            return residentRepository.findByNationalId(createDto.residentNationalId())
                    .map(Resident::getId)
                    .orElse(null);
        }
        if (createDto.residentPhone() != null && !createDto.residentPhone().isBlank()) {
            return residentRepository.findByPhone(createDto.residentPhone())
                    .map(Resident::getId)
                    .orElse(null);
        }
        if (createDto.residentEmail() != null && !createDto.residentEmail().isBlank()) {
            return residentRepository.findByEmail(createDto.residentEmail())
                    .map(Resident::getId)
                    .orElse(null);
        }
        return null;
    }

    private UUID ensureResidentExists(HouseholdMemberRequest request) {
        if (request.getResidentId() != null) {
            return residentRepository.findById(request.getResidentId())
                    .map(Resident::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        }

        Resident existing = null;
        if (request.getResidentNationalId() != null && !request.getResidentNationalId().isBlank()) {
            existing = residentRepository.findByNationalId(request.getResidentNationalId()).orElse(null);
        }
        if (existing == null && request.getResidentPhone() != null && !request.getResidentPhone().isBlank()) {
            existing = residentRepository.findByPhone(request.getResidentPhone()).orElse(null);
        }
        if (existing == null && request.getResidentEmail() != null && !request.getResidentEmail().isBlank()) {
            existing = residentRepository.findByEmail(request.getResidentEmail()).orElse(null);
        }

        if (existing == null) {
            Resident newResident = Resident.builder()
                    .fullName(request.getResidentFullName())
                    .phone(request.getResidentPhone())
                    .email(request.getResidentEmail())
                    .nationalId(request.getResidentNationalId())
                    .dob(request.getResidentDob())
                    .build();
            existing = residentRepository.save(newResident);
            log.info("Created resident {} from household member request {}", existing.getId(), request.getId());
        }

        request.setResidentId(existing.getId());
        return existing.getId();
    }
}
