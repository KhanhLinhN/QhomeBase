package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdMemberCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberUpdateDto;
import com.QhomeBase.baseservice.service.HouseHoldMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/household-members")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberController {

    private final HouseHoldMemberService householdMemberService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HouseholdMemberDto> createHouseholdMember(@Valid @RequestBody HouseholdMemberCreateDto createDto) {
        try {
            HouseholdMemberDto result = householdMemberService.createHouseholdMember(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create household member: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HouseholdMemberDto> updateHouseholdMember(
            @PathVariable UUID id,
            @Valid @RequestBody HouseholdMemberUpdateDto updateDto) {
        try {
            HouseholdMemberDto result = householdMemberService.updateHouseholdMember(id, updateDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update household member {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHouseholdMember(@PathVariable UUID id) {
        try {
            householdMemberService.deleteHouseholdMember(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete household member {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<HouseholdMemberDto> getHouseholdMemberById(@PathVariable UUID id) {
        try {
            HouseholdMemberDto result = householdMemberService.getHouseholdMemberById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get household member {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/households/{householdId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<HouseholdMemberDto>> getActiveMembersByHouseholdId(@PathVariable UUID householdId) {
        try {
            List<HouseholdMemberDto> result = householdMemberService.getActiveMembersByHouseholdId(householdId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to get members for household {}: {}", householdId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/residents/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<HouseholdMemberDto>> getActiveMembersByResidentId(@PathVariable UUID residentId) {
        try {
            List<HouseholdMemberDto> result = householdMemberService.getActiveMembersByResidentId(residentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to get households for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

