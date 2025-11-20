package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.HouseholdMemberCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberUpdateDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseHoldMemberService {
    
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public HouseholdMemberDto createHouseholdMember(HouseholdMemberCreateDto createDto) {
        Household household = householdRepository.findById(createDto.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        residentRepository.findById(createDto.residentId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));

        Optional<HouseholdMember> existingMember = householdMemberRepository
                .findMemberByResidentAndUnit(createDto.residentId(), household.getUnitId());

        if (existingMember.isPresent() && 
            existingMember.get().getHouseholdId().equals(createDto.householdId()) &&
            (existingMember.get().getLeftAt() == null || existingMember.get().getLeftAt().isAfter(LocalDate.now()))) {
            throw new IllegalArgumentException("Resident is already a member of this household");
        }

        if (createDto.isPrimary() != null && createDto.isPrimary()) {
            Optional<HouseholdMember> primaryMember = householdMemberRepository
                    .findPrimaryMemberByHouseholdId(createDto.householdId());
            if (primaryMember.isPresent()) {
                throw new IllegalArgumentException("Household already has a primary member");
            }
        }

        validateHouseholdCapacity(household);

        LocalDate joinedAt = createDto.joinedAt() != null ? createDto.joinedAt() : LocalDate.now();

        HouseholdMember member = HouseholdMember.builder()
                .householdId(createDto.householdId())
                .residentId(createDto.residentId())
                .relation(createDto.relation())
                .proofOfRelationImageUrl(createDto.proofOfRelationImageUrl())
                .isPrimary(createDto.isPrimary() != null ? createDto.isPrimary() : false)
                .joinedAt(joinedAt)
                .build();

        HouseholdMember savedMember = householdMemberRepository.save(member);
        log.info("Created household member {} for household {}", savedMember.getId(), createDto.householdId());

        return toDto(savedMember);
    }

    @Transactional
    public HouseholdMemberDto updateHouseholdMember(UUID memberId, HouseholdMemberUpdateDto updateDto) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));

        if (updateDto.householdId() != null) {
            householdRepository.findById(updateDto.householdId())
                    .orElseThrow(() -> new IllegalArgumentException("Household not found"));
            member.setHouseholdId(updateDto.householdId());
        }

        if (updateDto.residentId() != null) {
            residentRepository.findById(updateDto.residentId())
                    .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
            member.setResidentId(updateDto.residentId());
        }

        if (updateDto.relation() != null) {
            member.setRelation(updateDto.relation());
        }

        if (updateDto.proofOfRelationImageUrl() != null) {
            member.setProofOfRelationImageUrl(updateDto.proofOfRelationImageUrl());
        }

        if (updateDto.isPrimary() != null) {
            if (updateDto.isPrimary()) {
                Optional<HouseholdMember> existingPrimary = householdMemberRepository
                        .findPrimaryMemberByHouseholdId(member.getHouseholdId());
                if (existingPrimary.isPresent() && !existingPrimary.get().getId().equals(memberId)) {
                    throw new IllegalArgumentException("Household already has a primary member");
                }
            }
            member.setIsPrimary(updateDto.isPrimary());
        }

        if (updateDto.joinedAt() != null) {
            member.setJoinedAt(updateDto.joinedAt());
        }

        if (updateDto.leftAt() != null) {
            LocalDate joinedAt = updateDto.joinedAt() != null ? updateDto.joinedAt() : member.getJoinedAt();
            if (joinedAt != null && updateDto.leftAt().isBefore(joinedAt)) {
                throw new IllegalArgumentException("Left date cannot be before joined date");
            }
            member.setLeftAt(updateDto.leftAt());
        }

        HouseholdMember savedMember = householdMemberRepository.save(member);
        log.info("Updated household member {}", memberId);

        return toDto(savedMember);
    }

    @Transactional
    public void deleteHouseholdMember(UUID memberId) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));

        member.setLeftAt(LocalDate.now());
        householdMemberRepository.save(member);
        log.info("Removed household member {} from household", memberId);
    }

    private void validateHouseholdCapacity(Household household) {
        if (household == null) {
            throw new IllegalArgumentException("Household information is required");
        }

        Unit unit = unitRepository.findById(household.getUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found for this household"));

        long activeMembers = householdMemberRepository.countActiveMembersByHouseholdId(household.getId());
        int capacity = calculateCapacity(unit);

        if (activeMembers >= capacity) {
            String unitLabel = unit.getCode() != null ? unit.getCode() : "Căn hộ";
            throw new IllegalArgumentException(String.format(
                    "%s chỉ được đăng ký tối đa %d thành viên đang sinh sống (quy tắc 1 phòng ngủ x2). Vui lòng cập nhật lại danh sách trước khi thêm mới.",
                    unitLabel,
                    capacity
            ));
        }
    }

    private int calculateCapacity(Unit unit) {
        Integer bedrooms = unit.getBedrooms();
        int effectiveBedrooms = (bedrooms == null || bedrooms <= 0) ? 1 : bedrooms;
        return Math.max(1, effectiveBedrooms) * 2;
    }

    @Transactional(readOnly = true)
    public HouseholdMemberDto getHouseholdMemberById(UUID memberId) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));
        return toDto(member);
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberDto> getActiveMembersByHouseholdId(UUID householdId) {
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(householdId);
        return members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberDto> getActiveMembersByResidentId(UUID residentId) {
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByResidentId(residentId);
        return members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HouseholdMemberDto toDto(HouseholdMember member) {
        if (member == null) {
            return null;
        }

        String residentName = null;
        String residentEmail = null;
        String residentPhone = null;

        Resident resident = residentRepository.findById(member.getResidentId()).orElse(null);
        if (resident != null) {
            residentName = resident.getFullName();
            residentEmail = resident.getEmail();
            residentPhone = resident.getPhone();
        }

        return new HouseholdMemberDto(
                member.getId(),
                member.getHouseholdId(),
                member.getResidentId(),
                residentName,
                residentEmail,
                residentPhone,
                member.getRelation(),
                member.getProofOfRelationImageUrl(),
                member.getIsPrimary(),
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
