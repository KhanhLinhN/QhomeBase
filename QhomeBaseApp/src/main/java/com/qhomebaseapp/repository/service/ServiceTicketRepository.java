package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, Long> {
    
    List<ServiceTicket> findByService_IdAndIsActiveTrue(Long serviceId);
    
    List<ServiceTicket> findByService_CodeAndIsActiveTrue(String serviceCode);
    
    ServiceTicket findByService_IdAndCode(Long serviceId, String code);
    
    List<ServiceTicket> findByService_IdAndTicketTypeAndIsActiveTrue(Long serviceId, String ticketType);
}

