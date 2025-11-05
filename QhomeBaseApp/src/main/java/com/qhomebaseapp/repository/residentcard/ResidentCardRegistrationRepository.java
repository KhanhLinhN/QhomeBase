package com.qhomebaseapp.repository.residentcard;

import com.qhomebaseapp.model.ResidentCardRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidentCardRegistrationRepository extends JpaRepository<ResidentCardRegistration, Long> {
    
    List<ResidentCardRegistration> findByUser_Id(Long userId);
    
    Page<ResidentCardRegistration> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

