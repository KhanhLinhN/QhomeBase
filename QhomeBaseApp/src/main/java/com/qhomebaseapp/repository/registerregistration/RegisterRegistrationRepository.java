package com.qhomebaseapp.repository.registerregistration;

import com.qhomebaseapp.model.RegisterServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegisterRegistrationRepository extends JpaRepository<RegisterServiceRequest, Long> {

    // Tìm tất cả request theo user
    List<RegisterServiceRequest> findByUser_Id(Long userId);

}
