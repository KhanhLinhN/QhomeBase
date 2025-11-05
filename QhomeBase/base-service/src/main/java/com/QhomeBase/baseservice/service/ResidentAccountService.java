package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResidentAccountService {
    
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UnitRepository unitRepository;
    private final IamClientService iamClientService;
    private final AccountCreationRequestRepository accountCreationRequestRepository;
    
    public boolean canCreateAccountForUnit(UUID unitId, UUID requesterUserId) {
        Household household = householdRepository.findCurrentHouseholdByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit has no household"));
        
        if (household.getKind() != HouseholdKind.OWNER) {
            return false;
        }
        
        if (household.getPrimaryResidentId() != null) {
            Resident primaryResident = residentRepository.findById(household.getPrimaryResidentId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary resident not found"));
            
            if (primaryResident.getUserId() != null && 
                primaryResident.getUserId().equals(requesterUserId)) {
                return true;
            }
        }
        
        HouseholdMember primaryMember = householdMemberRepository
                .findPrimaryMemberByHouseholdId(household.getId())
                .orElse(null);
        
        if (primaryMember != null) {
            Resident member = residentRepository.findById(primaryMember.getResidentId())
                    .orElse(null);
            
            if (member != null && member.getUserId() != null && 
                member.getUserId().equals(requesterUserId)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Transactional(readOnly = true)
    public List<ResidentWithoutAccountDto> getResidentsWithoutAccount(UUID unitId, UUID requesterUserId) {
        if (!canCreateAccountForUnit(unitId, requesterUserId)) {
            throw new IllegalArgumentException("You don't have permission to view residents in this unit");
        }
        
        Household household = householdRepository.findCurrentHouseholdByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit has no household"));
        
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(household.getId());
        
        return members.stream()
                .map(member -> {
                    Resident resident = residentRepository.findById(member.getResidentId())
                            .orElse(null);
                    if (resident != null && resident.getUserId() == null) {
                        return new ResidentWithoutAccountDto(
                                resident.getId(),
                                resident.getFullName(),
                                resident.getPhone(),
                                resident.getEmail(),
                                resident.getNationalId(),
                                resident.getDob(),
                                resident.getStatus(),
                                member.getRelation(),
                                member.getIsPrimary()
                        );
                    }
                    return null;
                })
                .filter(resident -> resident != null)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ResidentAccountDto createAccountForResident(UUID residentId, CreateResidentAccountDto request, UUID requesterUserId) {
        return createAccountForResident(residentId, request, requesterUserId, null);
    }
    
    @Transactional
    public ResidentAccountDto createAccountForResident(UUID residentId, CreateResidentAccountDto request, UUID requesterUserId, String token) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        
        if (resident.getUserId() != null) {
            throw new IllegalArgumentException("Resident already has an account");
        }
        
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByResidentId(residentId);
        
        if (members.isEmpty()) {
            throw new IllegalArgumentException("Resident does not belong to any household");
        }
        
        Household household = householdRepository.findById(members.get(0).getHouseholdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));
        
        if (!canCreateAccountForUnit(household.getUnitId(), requesterUserId)) {
            throw new IllegalArgumentException("You don't have permission to create account for this resident");
        }
        
        String username;
        String password = null;
        
        if (request.autoGenerate()) {
            if (resident.getEmail() != null && !resident.getEmail().isEmpty()) {
                username = resident.getEmail().split("@")[0];
            } else if (resident.getPhone() != null && !resident.getPhone().isEmpty()) {
                username = "resident_" + resident.getPhone().replaceAll("[^0-9]", "");
            } else {
                username = "resident_" + resident.getId().toString().substring(0, 8);
            }
            
            int counter = 1;
            String originalUsername = username;
            while (iamClientService.usernameExists(username, token)) {
                username = originalUsername + counter;
                counter++;
            }
        } else {
            username = request.username();
            password = request.password();
            
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username is required when autoGenerate is false");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required when autoGenerate is false");
            }
        }
        
        String email = resident.getEmail() != null && !resident.getEmail().isEmpty() 
                ? resident.getEmail() 
                : username + "@qhome.local";
        
        if (iamClientService.usernameExists(username, token)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        if (iamClientService.emailExists(email, token)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        ResidentAccountDto accountDto = iamClientService.createUserForResident(
                username,
                email,
                password,
                request.autoGenerate(),
                residentId,
                token
        );
        
        resident.setUserId(accountDto.userId());
        residentRepository.save(resident);
        
        log.info("Created account for resident {}: userId={}, username={}", 
                residentId, accountDto.userId(), accountDto.username());
        
        return accountDto;
    }
    
    @Transactional(readOnly = true)
    public ResidentAccountDto getResidentAccount(UUID residentId, UUID requesterUserId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        
        if (resident.getUserId() == null) {
            return null;
        }
        
        if (!resident.getUserId().equals(requesterUserId)) {
            List<HouseholdMember> members = householdMemberRepository
                    .findActiveMembersByResidentId(residentId);
            
            if (!members.isEmpty()) {
                Household household = householdRepository.findById(members.get(0).getHouseholdId())
                        .orElse(null);
                
                if (household != null && !canCreateAccountForUnit(household.getUnitId(), requesterUserId)) {
                    throw new IllegalArgumentException("You don't have permission to view this account");
                }
            }
        }
        
        ResidentAccountDto accountDto = iamClientService.getUserAccountInfo(resident.getUserId());
        
        if (accountDto == null) {
            throw new IllegalArgumentException("User account not found");
        }
        
        return accountDto;
    }

    @Transactional
    public AccountCreationRequestDto createAccountRequest(CreateAccountRequestDto request, UUID requesterUserId) {
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));

        if (resident.getUserId() != null) {
            throw new IllegalArgumentException("Resident already has an account");
        }

        AccountCreationRequest existingRequest = accountCreationRequestRepository
                .findPendingByResidentId(request.residentId())
                .orElse(null);

        if (existingRequest != null) {
            throw new IllegalArgumentException("There is already a pending request for this resident");
        }

        List<HouseholdMember> residentMembers = householdMemberRepository
                .findActiveMembersByResidentId(request.residentId());

        if (residentMembers.isEmpty()) {
            throw new IllegalArgumentException("Resident does not belong to any household");
        }

        boolean hasPermission = false;
        for (HouseholdMember member : residentMembers) {
            Household household = householdRepository.findById(member.getHouseholdId())
                    .orElse(null);
            if (household != null && canCreateAccountForUnit(household.getUnitId(), requesterUserId)) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("You don't have permission to create account request for this resident");
        }

        String username = null;
        String password = null;
        String email = resident.getEmail() != null && !resident.getEmail().isEmpty()
                ? resident.getEmail()
                : null;

        if (!request.autoGenerate()) {
            username = request.username();
            password = request.password();
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username is required when autoGenerate is false");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required when autoGenerate is false");
            }
            
            if (!username.matches("^[a-zA-Z0-9_-]+$")) {
                throw new IllegalArgumentException("Username can only contain letters, numbers, underscore, and hyphen");
            }
            
            if (iamClientService.usernameExists(username)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
        }

        AccountCreationRequest accountRequest = AccountCreationRequest.builder()
                .residentId(request.residentId())
                .requestedBy(requesterUserId)
                .username(username)
                .password(password)
                .email(email)
                .autoGenerate(request.autoGenerate())
                .proofOfRelationImageUrl(request.proofOfRelationImageUrl())
                .status(AccountCreationRequest.RequestStatus.PENDING)
                .build();

        AccountCreationRequest savedRequest = accountCreationRequestRepository.save(accountRequest);

        log.info("Created account request for resident {} by user {}", request.residentId(), requesterUserId);

        return mapToDto(savedRequest);
    }

    @Transactional
    public AccountCreationRequestDto approveAccountRequest(UUID requestId, UUID adminUserId, boolean approve, String rejectionReason) {
        return approveAccountRequest(requestId, adminUserId, approve, rejectionReason, null);
    }
    
    @Transactional
    public AccountCreationRequestDto approveAccountRequest(UUID requestId, UUID adminUserId, boolean approve, String rejectionReason, String token) {
        AccountCreationRequest request = accountCreationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Account creation request not found"));

        if (request.getStatus() != AccountCreationRequest.RequestStatus.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        if (approve) {
            request.setStatus(AccountCreationRequest.RequestStatus.APPROVED);
            request.setApprovedBy(adminUserId);
            request.setApprovedAt(OffsetDateTime.now());

            accountCreationRequestRepository.save(request);

            CreateResidentAccountDto createRequest = new CreateResidentAccountDto(
                    request.getUsername(),
                    request.getPassword(),
                    request.getAutoGenerate()
            );

            try {
                log.info("Attempting to create account for resident {} with autoGenerate={}, username={}", 
                        request.getResidentId(), request.getAutoGenerate(), request.getUsername());
                
                ResidentAccountDto account = createAccountForResident(
                        request.getResidentId(),
                        createRequest,
                        request.getRequestedBy(),
                        token
                );

                log.info("Approved and created account for resident {}: userId={}, username={}", 
                        request.getResidentId(), account.userId(), account.username());
            } catch (Exception e) {
                log.error("Failed to create account after approval for resident {}: {}", 
                        request.getResidentId(), e.getMessage(), e);
                request.setStatus(AccountCreationRequest.RequestStatus.REJECTED);
                request.setRejectedBy(adminUserId);
                request.setRejectionReason("Failed to create account: " + e.getMessage());
                request.setRejectedAt(OffsetDateTime.now());
                accountCreationRequestRepository.save(request);
                throw new RuntimeException("Failed to create account after approval: " + e.getMessage(), e);
            }
        } else {
            request.setStatus(AccountCreationRequest.RequestStatus.REJECTED);
            request.setRejectedBy(adminUserId);
            request.setRejectionReason(rejectionReason);
            request.setRejectedAt(OffsetDateTime.now());

            accountCreationRequestRepository.save(request);
            log.info("Rejected account request {} by admin {}", requestId, adminUserId);
        }

        return mapToDto(request);
    }

    @Transactional(readOnly = true)
    public List<AccountCreationRequestDto> getPendingRequests() {
        List<AccountCreationRequest> requests = accountCreationRequestRepository.findAllPendingRequests();
        return requests.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountCreationRequestDto> getMyRequests(UUID requesterUserId) {
        List<AccountCreationRequest> requests = accountCreationRequestRepository.findByRequestedBy(requesterUserId);
        return requests.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AccountCreationRequestDto mapToDto(AccountCreationRequest request) {
        Resident resident = residentRepository.findById(request.getResidentId())
                .orElse(null);

        Resident requestedByResident = residentRepository.findByUserId(request.getRequestedBy())
                .orElse(null);

        Resident approvedByResident = request.getApprovedBy() != null
                ? residentRepository.findByUserId(request.getApprovedBy()).orElse(null)
                : null;

        Resident rejectedByResident = request.getRejectedBy() != null
                ? residentRepository.findByUserId(request.getRejectedBy()).orElse(null)
                : null;

        UUID householdId = null;
        UUID unitId = null;
        String unitCode = null;
        String relation = null;

        if (resident != null) {
            List<HouseholdMember> members = householdMemberRepository
                    .findActiveMembersByResidentId(resident.getId());
            if (!members.isEmpty()) {
                HouseholdMember member = members.get(0);
                householdId = member.getHouseholdId();
                relation = member.getRelation();

                Household household = householdRepository.findById(householdId).orElse(null);
                if (household != null) {
                    unitId = household.getUnitId();
                    Unit unit = unitRepository.findById(unitId).orElse(null);
                    if (unit != null) {
                        unitCode = unit.getCode();
                    }
                }
            }
        }

        return new AccountCreationRequestDto(
                request.getId(),
                request.getResidentId(),
                resident != null ? resident.getFullName() : null,
                resident != null ? resident.getEmail() : null,
                resident != null ? resident.getPhone() : null,
                householdId,
                unitId,
                unitCode,
                relation,
                request.getRequestedBy(),
                requestedByResident != null ? requestedByResident.getFullName() : null,
                request.getUsername(),
                request.getEmail(),
                request.getAutoGenerate(),
                request.getStatus(),
                request.getApprovedBy(),
                approvedByResident != null ? approvedByResident.getFullName() : null,
                request.getRejectedBy(),
                rejectedByResident != null ? rejectedByResident.getFullName() : null,
                request.getRejectionReason(),
                request.getProofOfRelationImageUrl(),
                request.getApprovedAt(),
                request.getRejectedAt(),
                request.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<UnitDto> getMyUnits(UUID userId) {
        List<Unit> units = unitRepository.findAllUnitsByUserId(userId);
        
        return units.stream()
                .map(unit -> {
                    UUID primaryResidentId = householdRepository.findCurrentHouseholdByUnitId(unit.getId())
                            .map(Household::getPrimaryResidentId)
                            .orElse(null);
                    
                    return new UnitDto(
                            unit.getId(),
                            unit.getBuilding().getId(),
                            unit.getBuilding().getCode(),
                            unit.getBuilding().getName(),
                            unit.getCode(),
                            unit.getFloor(),
                            unit.getAreaM2(),
                            unit.getBedrooms(),
                            unit.getStatus(),
                            primaryResidentId,
                            unit.getCreatedAt(),
                            unit.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean canManageUnit(UUID userId, UUID unitId) {
        return canCreateAccountForUnit(unitId, userId);
    }

    @Transactional(readOnly = true)
    public UnitAccessDto getUnitAccessInfo(UUID userId, UUID unitId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElse(null);
        
        if (resident == null) {
            return null;
        }
        
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByResidentId(resident.getId());

        if (members == null || members.isEmpty()) {
            return null;
        }

        for (HouseholdMember member : members) {
            Household household = householdRepository.findById(member.getHouseholdId())
                    .orElse(null);
            
            if (household != null && household.getUnitId().equals(unitId)) {
                boolean isPrimary = member.getIsPrimary() != null && member.getIsPrimary();
                
                if (!isPrimary && household.getPrimaryResidentId() != null) {
                    Resident primaryResident = residentRepository.findById(household.getPrimaryResidentId())
                            .orElse(null);
                    if (primaryResident != null && primaryResident.getUserId() != null && 
                        primaryResident.getUserId().equals(userId)) {
                        isPrimary = true;
                    }
                }
                
                Unit unit = unitRepository.findByIdWithBuilding(unitId);
                if (unit == null) {
                    return null;
                }
                
                return new UnitAccessDto(
                        unitId,
                        unit.getCode(),
                        isPrimary,
                        member.getRelation()
                );
            }
        }
        
        return null;
    }
}

