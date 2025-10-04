package com.qhomebaseapp.repository.registerregistration;

import com.qhomebaseapp.model.RegisterServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegisterRegistrationRepository{
    RegisterServiceRequest save(RegisterServiceRequest request);
    List<RegisterServiceRequest> findByUserId(Long userId);
}