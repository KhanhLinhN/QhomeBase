package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    
    List<Vehicle> findAllByTenantId(UUID tenantId);
    
    List<Vehicle> findAllByTenantIdAndActive(UUID tenantId, Boolean active);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.resident WHERE v.tenantId = :tenantId")
    List<Vehicle> findAllByTenantIdWithResident(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.unit WHERE v.tenantId = :tenantId")
    List<Vehicle> findAllByTenantIdWithUnit(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.resident JOIN FETCH v.unit WHERE v.tenantId = :tenantId")
    List<Vehicle> findAllByTenantIdWithResidentAndUnit(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.resident WHERE v.resident.id = :residentId")
    List<Vehicle> findAllByResidentId(@Param("residentId") UUID residentId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.unit WHERE v.unit.id = :unitId")
    List<Vehicle> findAllByUnitId(@Param("unitId") UUID unitId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.resident JOIN FETCH v.unit WHERE v.id = :id")
    Vehicle findByIdWithResidentAndUnit(@Param("id") UUID id);
    
    boolean existsByTenantIdAndPlateNo(UUID tenantId, String plateNo);
    
    boolean existsByTenantIdAndPlateNoAndIdNot(UUID tenantId, String plateNo, UUID id);
    
    boolean existsByTenantIdAndResidentId(UUID tenantId, UUID residentId);
    
    List<Vehicle> findAllByTenantIdAndActiveTrue(UUID tenantId);
}
