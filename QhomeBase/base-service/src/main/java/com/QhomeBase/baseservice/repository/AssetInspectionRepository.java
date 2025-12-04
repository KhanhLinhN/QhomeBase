package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.AssetInspection;
import com.QhomeBase.baseservice.model.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetInspectionRepository extends JpaRepository<AssetInspection, UUID> {

    Optional<AssetInspection> findByContractId(UUID contractId);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.unit.id = :unitId")
    List<AssetInspection> findByUnitId(@Param("unitId") UUID unitId);

    List<AssetInspection> findByStatus(InspectionStatus status);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.unit.id = :unitId AND ai.status = :status")
    List<AssetInspection> findByUnitIdAndStatus(@Param("unitId") UUID unitId, @Param("status") InspectionStatus status);
}

