package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceAvailabilityRepository extends JpaRepository<ServiceAvailability, Long> {
    List<ServiceAvailability> findByService_IdAndIsAvailableTrue(Long serviceId);
    List<ServiceAvailability> findByService_Id(Long serviceId);
}

