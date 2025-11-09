package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOptionGroupRepository extends JpaRepository<ServiceOptionGroup, UUID> {

    List<ServiceOptionGroup> findAllByServiceId(UUID serviceId);

    boolean existsByServiceIdAndCodeIgnoreCase(UUID serviceId, String code);

    boolean existsByServiceIdAndCodeIgnoreCaseAndIdNot(UUID serviceId, String code, UUID excludeId);

    Optional<ServiceOptionGroup> findByIdAndServiceId(UUID id, UUID serviceId);
}





