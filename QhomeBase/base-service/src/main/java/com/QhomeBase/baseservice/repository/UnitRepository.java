package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Unit;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
    public List<Unit> findAllByTenantId(UUID tenantId);


    @Query("SELECT u FROM Unit u WHERE u.building.id = :buildingId")
    List<Unit> findAllByBuildingId(@Param("buildingId") UUID buildingId);
}
