package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;

import java.util.List;

public interface RegisterRegistrationService {

    RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId);

    List<RegisterServiceRequestResponseDto> getByUserId(Long userId);
}
