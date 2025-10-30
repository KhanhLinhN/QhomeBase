package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service to manage households and primary residents (chủ hộ).
 * Primary resident is responsible for all payments related to the unit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HouseholdService {
    
    private final HouseholdRepository householdRepository;

    public Optional<UUID> getPrimaryResidentForUnit(UUID unitId) {
        return householdRepository.findCurrentHouseholdByUnitId(unitId)
                .map(Household::getPrimaryResidentId);
    }

    public boolean isPrimaryResident(UUID unitId, UUID userId) {
        return getPrimaryResidentForUnit(unitId)
                .map(primaryId -> primaryId.equals(userId))
                .orElse(false);
    }

    public UUID getPayerForUnit(UUID unitId) {
        return getPrimaryResidentForUnit(unitId).orElse(null);
    }
}




