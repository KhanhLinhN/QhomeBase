package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Building;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building,UUID> {
    List<Building> findAllByTenantIdOrderByCodeAsc(UUID tenantId);

    @Query(value = """
    SELECT t.code
    FROM data.Buildings b
    JOIN data.tenants t ON t.id = b.tenant_id
    WHERE b.id = :buildingId
    LIMIT 1
""", nativeQuery = true)
    String findTenantCodeByBuilding(@Param("buildingId") UUID buildingId);


    @Query(value = "select t.code from data.tenants t where t.id = :tenantId", nativeQuery = true)
    Optional<String> findTenantCodeByTenantId(@Param("tenantId") UUID tenantId);


    @Query(value = """
    SELECT t.id
    FROM data.Buildings b
    JOIN data.tenants t ON t.id = b.tenant_id
    WHERE b.id = :buildingId
    LIMIT 1
""", nativeQuery = true)
    UUID findTenantIdByBuilding(@Param("buildingId") UUID buildingId);

}

