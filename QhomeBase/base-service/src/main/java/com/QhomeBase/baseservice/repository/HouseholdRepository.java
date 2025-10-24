package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {

    @Query("SELECT h FROM Household h " +
            "WHERE h.unitId = :unitId AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE) ORDER BY h.startDate DESC")
    Optional<Household> findCurrentHouseholdByUnitId(@Param("unitId") UUID unitId);
}

