package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.BarSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BarSlotRepository extends JpaRepository<BarSlot, Long> {
    
    List<BarSlot> findByService_IdAndIsActiveTrueOrderBySortOrderAsc(Long serviceId);
    
    List<BarSlot> findByService_CodeAndIsActiveTrueOrderBySortOrderAsc(String serviceCode);
    
    BarSlot findByService_IdAndCode(Long serviceId, String code);
}

