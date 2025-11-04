package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceComboRepository extends JpaRepository<ServiceCombo, Long> {
    
    List<ServiceCombo> findByService_IdAndIsActiveTrue(Long serviceId);
    
    List<ServiceCombo> findByService_CodeAndIsActiveTrue(String serviceCode);
    
    ServiceCombo findByService_IdAndCode(Long serviceId, String code);
}

