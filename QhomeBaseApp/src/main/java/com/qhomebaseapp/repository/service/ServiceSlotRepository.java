package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceSlotRepository extends JpaRepository<ServiceSlot, Long> {
    
    List<ServiceSlot> findByService_IdAndIsActiveTrueOrderBySortOrderAsc(Long serviceId);
    
    List<ServiceSlot> findByService_CodeAndIsActiveTrueOrderBySortOrderAsc(String serviceCode);
    
    java.util.Optional<ServiceSlot> findByService_IdAndCode(Long serviceId, String code);
    
    List<ServiceSlot> findByService_IdOrderBySortOrderAsc(Long serviceId);
}

