package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.HouseholdMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    
    @Query("SELECT hm FROM HouseholdMember hm " +
           "WHERE hm.householdId = :householdId " +
           "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
    List<HouseholdMember> findActiveMembersByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT hm FROM HouseholdMember hm " +
           "WHERE hm.householdId = :householdId " +
           "AND hm.isPrimary = true " +
           "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
    Optional<HouseholdMember> findPrimaryMemberByHouseholdId(@Param("householdId") UUID householdId);
    
    @Query("SELECT hm FROM HouseholdMember hm " +
           "WHERE hm.residentId = :residentId " +
           "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
    List<HouseholdMember> findActiveMembersByResidentId(@Param("residentId") UUID residentId);
    
    @Query("SELECT hm FROM HouseholdMember hm " +
           "JOIN Household h ON hm.householdId = h.id " +
           "WHERE hm.residentId = :residentId " +
           "AND h.unitId = :unitId " +
           "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
    Optional<HouseholdMember> findMemberByResidentAndUnit(
            @Param("residentId") UUID residentId,
            @Param("unitId") UUID unitId
    );
}

