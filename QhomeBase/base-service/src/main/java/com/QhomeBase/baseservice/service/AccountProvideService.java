package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.CreateResidentAccountDto;
import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionRequest;
import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionResponse;
import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountProvideService {

    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ResidentAccountService residentAccountService;

    @Transactional
    public PrimaryResidentProvisionResponse provisionPrimaryResident(
            UUID householdId,
            PrimaryResidentProvisionRequest request,
            String token
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        if (household.getPrimaryResidentId() != null) {
            throw new IllegalStateException("Household already has a primary resident");
        }

        validateUniqueContact(request);

        Resident.ResidentBuilder builder = Resident.builder()
                .fullName(request.resident().fullName())
                .phone(request.resident().phone())
                .email(request.resident().email())
                .nationalId(request.resident().nationalId())
                .dob(request.resident().dob());

        if (request.resident().status() != null) {
            builder.status(request.resident().status());
        }

        Resident resident = builder.build();

        resident = residentRepository.save(resident);

        household.setPrimaryResidentId(resident.getId());
        household.setUpdatedAt(OffsetDateTime.now());
        householdRepository.save(household);

        HouseholdMember householdMember = HouseholdMember.builder()
                .householdId(householdId)
                .residentId(resident.getId())
                .relation(resolveRelation(request))
                .isPrimary(true)
                .joinedAt(LocalDate.now())
                .build();

        householdMember = householdMemberRepository.save(householdMember);

        CreateResidentAccountDto accountRequest = request.account();
        if (accountRequest == null) {
            accountRequest = new CreateResidentAccountDto(null, null, true);
        }

        ResidentAccountDto account = residentAccountService.createAccountForResidentAsAdmin(
                resident.getId(),
                accountRequest,
                token
        );

        log.info("Provisioned primary resident {} for household {}", resident.getId(), householdId);

        return new PrimaryResidentProvisionResponse(
                resident.getId(),
                householdMember.getId(),
                account
        );
    }

    private void validateUniqueContact(PrimaryResidentProvisionRequest request) {
        String phone = request.resident().phone();
        if (phone != null && !phone.isBlank() && residentRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("There is already a resident with that phone");
        }

        String email = request.resident().email();
        if (email != null && !email.isBlank() && residentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("There is already a resident with that email");
        }

        String nationalId = request.resident().nationalId();
        if (nationalId != null && !nationalId.isBlank() && residentRepository.existsByNationalId(nationalId)) {
            throw new IllegalArgumentException("There is already a resident with that national id");
        }
    }

    private String resolveRelation(PrimaryResidentProvisionRequest request) {
        String relation = request.relation();
        if (relation == null || relation.isBlank()) {
            return "Chủ hộ";
        }
        return relation;
    }
}
