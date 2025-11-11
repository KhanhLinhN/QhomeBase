package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

    Optional<ServiceCategory> findTopByOrderBySortOrderDesc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
