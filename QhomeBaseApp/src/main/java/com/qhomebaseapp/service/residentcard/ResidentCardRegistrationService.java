package com.qhomebaseapp.service.residentcard;

import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationDto;
import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ResidentCardRegistrationService {

    ResidentCardRegistrationResponseDto registerResidentCard(ResidentCardRegistrationDto dto, Long userId);

    List<ResidentCardRegistrationResponseDto> getByUserId(Long userId);

    ResidentCardRegistrationResponseDto getById(Long id, Long userId);

    Page<ResidentCardRegistrationResponseDto> getByUserIdPaginated(Long userId, int page, int size);

    Map<String, Object> createVnpayPaymentUrlWithData(ResidentCardRegistrationDto dto, Long userId, HttpServletRequest request);

    String createVnpayPaymentUrl(Long registrationId, Long userId, HttpServletRequest request);

    void handleVnpayCallback(Long registrationId, Map<String, String> vnpParams);

    void cancelRegistration(Long registrationId, Long userId);
}

