package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeterRepository extends JpaRepository<Meter, UUID> {
    
    Optional<Meter> findByMeterCode(String meterCode);
    
    List<Meter> findByUnitId(UUID unitId);
    
    List<Meter> findByServiceId(UUID serviceId);
    
    @Query("SELECT m FROM Meter m WHERE m.unit.id = :unitId AND m.service.id = :serviceId AND m.active = true")
    Optional<Meter> findByUnitAndService(@Param("unitId") UUID unitId, @Param("serviceId") UUID serviceId);
    
    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.building.id = :buildingId
          AND m.service.id = :serviceId
          AND u.floor BETWEEN :floorFrom AND :floorTo
          AND m.active = true
        ORDER BY u.floor, u.code
    """)
    List<Meter> findByBuildingServiceAndFloorRange(
        @Param("buildingId") UUID buildingId,
        @Param("serviceId") UUID serviceId,
        @Param("floorFrom") Integer floorFrom,
        @Param("floorTo") Integer floorTo
    );
    
    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.building.id = :buildingId
          AND m.service.id = :serviceId
          AND m.active = true
        ORDER BY u.floor, u.code
    """)
    List<Meter> findByBuildingAndService(
        @Param("buildingId") UUID buildingId,
        @Param("serviceId") UUID serviceId
    );
}

