package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.CreateResidentAccountDto;
import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.dto.ResidentDto;
import com.QhomeBase.baseservice.dto.ResidentWithoutAccountDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdKind;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final UserRepository userRepository; // From iam-service
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    /**
     * Check if requester has permission to create account for residents in a unit
     */
    public boolean canCreateAccountForUnit(UUID unitId, UUID requesterUserId) {
        // Find current household of unit
        Household household = householdRepository.findCurrentHouseholdByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit has no household"));
        
        // Only allow for OWNER households
        if (household.getKind() != HouseholdKind.OWNER) {
            return false;
        }
        
        // Check if requester is primary resident
        if (household.getPrimaryResidentId() != null) {
            Resident primaryResident = residentRepository.findById(household.getPrimaryResidentId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary resident not found"));
            
            if (primaryResident.getUserId() != null && 
                primaryResident.getUserId().equals(requesterUserId)) {
                return true;
            }
        }
        
        // If no primary resident, check if requester is primary member
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
    
    /**
     * Get list of residents in household without account
     */
    @Transactional(readOnly = true)
    public List<ResidentWithoutAccountDto> getResidentsWithoutAccount(UUID unitId, UUID requesterUserId) {
        // Check permission
        if (!canCreateAccountForUnit(unitId, requesterUserId)) {
            throw new IllegalArgumentException("You don't have permission to view residents in this unit");
        }
        
        // Find current household
        Household household = householdRepository.findCurrentHouseholdByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit has no household"));
        
        // Get active members
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(household.getId());
        
        // Filter residents without account
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
    
    /**
     * Create account for a resident
     */
    @Transactional
    public ResidentAccountDto createAccountForResident(UUID residentId, CreateResidentAccountDto request, UUID requesterUserId) {
        // Find resident
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        
        // Check if resident already has account
        if (resident.getUserId() != null) {
            throw new IllegalArgumentException("Resident already has an account");
        }
        
        // Check if requester has permission
        // Find which unit the resident belongs to
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
        
        // Generate username and password if auto-generate
        String username;
        String password;
        
        if (request.autoGenerate()) {
            // Generate username from email or phone
            if (resident.getEmail() != null && !resident.getEmail().isEmpty()) {
                username = resident.getEmail().split("@")[0];
            } else if (resident.getPhone() != null && !resident.getPhone().isEmpty()) {
                username = "resident_" + resident.getPhone().replaceAll("[^0-9]", "");
            } else {
                username = "resident_" + resident.getId().toString().substring(0, 8);
            }
            
            // Ensure username is unique
            int counter = 1;
            String originalUsername = username;
            while (userRepository.findByUsername(username).isPresent()) {
                username = originalUsername + counter;
                counter++;
            }
            
            password = generateRandomPassword(8);
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
        
        // Create user account
        String email = resident.getEmail() != null && !resident.getEmail().isEmpty() 
                ? resident.getEmail() 
                : username + "@qhome.local";
        
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Hash password
        String passwordHash = passwordEncoder.encode(password);
        
        // Create user
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .active(true)
                .build();
        
        // Add RESIDENT role
        user.addRole(UserRole.RESIDENT);
        
        User savedUser = userRepository.save(user);
        
        // Link resident to user
        resident.setUserId(savedUser.getId());
        residentRepository.save(resident);
        
        log.info("Created account for resident {}: userId={}, username={}", 
                residentId, savedUser.getId(), savedUser.getUsername());
        
        return new ResidentAccountDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRoles().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toList()),
                savedUser.isActive()
        );
    }
    
    /**
     * Get account info for a resident
     */
    @Transactional(readOnly = true)
    public ResidentAccountDto getResidentAccount(UUID residentId, UUID requesterUserId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        
        if (resident.getUserId() == null) {
            return null; // No account yet
        }
        
        // Check permission: requester must be the resident or have permission to view
        if (!resident.getUserId().equals(requesterUserId)) {
            // Check if requester has permission (primary resident)
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
        
        User user = userRepository.findById(resident.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
        
        return new ResidentAccountDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toList()),
                user.isActive()
        );
    }
    
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}

