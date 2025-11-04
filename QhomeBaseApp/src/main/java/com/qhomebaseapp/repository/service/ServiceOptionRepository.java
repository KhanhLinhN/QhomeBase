package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOptionRepository extends JpaRepository<ServiceOption, Long> {
    
    List<ServiceOption> findByService_IdAndIsActiveTrue(Long serviceId);
    
    List<ServiceOption> findByService_CodeAndIsActiveTrue(String serviceCode);
    
    ServiceOption findByService_IdAndCode(Long serviceId, String code);
    
    List<ServiceOption> findByService_IdAndCodeContaining(Long serviceId, String code);
}

