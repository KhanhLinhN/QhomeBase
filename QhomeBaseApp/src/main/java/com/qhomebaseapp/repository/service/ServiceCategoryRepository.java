package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByIsActiveTrueOrderBySortOrderAsc();
    Optional<ServiceCategory> findByCode(String code);
    Optional<ServiceCategory> findByCodeAndIsActiveTrue(String code);
}

