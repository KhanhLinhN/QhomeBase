package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building,UUID> {
    // All buildings are now under a single project
    List<Building> findAllByOrderByCodeAsc();

    Building getBuildingById(UUID id);

    Optional<Building> findByCode(String code);
}

