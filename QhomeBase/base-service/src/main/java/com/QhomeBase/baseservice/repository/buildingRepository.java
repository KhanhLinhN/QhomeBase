package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface buildingRepository extends JpaRepository<building,UUID> {
    List<building> findAllByTenantIdOrderByCodeAsc(UUID tenantId);;
}
