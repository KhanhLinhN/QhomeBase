package com.qhomebaseapp.repository.elevatorcard;

import com.qhomebaseapp.model.ElevatorCardRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElevatorCardRegistrationRepository extends JpaRepository<ElevatorCardRegistration, Long> {
    
    List<ElevatorCardRegistration> findByUser_Id(Long userId);
    
    Page<ElevatorCardRegistration> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

