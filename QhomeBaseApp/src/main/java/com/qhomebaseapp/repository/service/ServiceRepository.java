package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByCategory_IdAndIsActiveTrue(Long categoryId);
    List<Service> findByCategory_CodeAndIsActiveTrue(String categoryCode);
    Optional<Service> findByCode(String code);
    List<Service> findByIsActiveTrue();
}

