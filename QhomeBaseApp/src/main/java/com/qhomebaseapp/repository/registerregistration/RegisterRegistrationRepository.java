package com.qhomebaseapp.repository.registerregistration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.qhomebaseapp.model.RegisterServiceRequest;
import java.util.List;

@Repository
public interface RegisterRegistrationRepository extends JpaRepository<RegisterServiceRequest, Long> {
    List<RegisterServiceRequest> findByUser_Id(Long userId);
    Page<RegisterServiceRequest> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
