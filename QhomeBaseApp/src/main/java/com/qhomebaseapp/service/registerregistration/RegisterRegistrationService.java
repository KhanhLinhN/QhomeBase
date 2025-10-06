package com.qhomebaseapp.service.registerregistration;



import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;

import java.util.List;

public interface RegisterRegistrationService {
    RegisterServiceRequest registerService(RegisterServiceRequestDto dto);
    List<RegisterServiceRequest> getByUserId(Long userId);
}