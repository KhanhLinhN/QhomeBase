package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Unit;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.building.id = :buildingId")
    List<Unit> findAllByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.building.id = :buildingId AND u.floor = :floorNumber")
    List<Unit> findByBuildingIdAndFloorNumber(@Param("buildingId") UUID buildingId, @Param("floorNumber") int floorNumber);
    
    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.id = :id")
    Unit findByIdWithBuilding(@Param("id") UUID id);
}
