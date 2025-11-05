package com.qhomebaseapp.service.elevatorcard;

import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationDto;
import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ElevatorCardRegistrationService {

    ElevatorCardRegistrationResponseDto registerElevatorCard(ElevatorCardRegistrationDto dto, Long userId);

    List<ElevatorCardRegistrationResponseDto> getByUserId(Long userId);

    ElevatorCardRegistrationResponseDto getById(Long id, Long userId);

    Page<ElevatorCardRegistrationResponseDto> getByUserIdPaginated(Long userId, int page, int size);

    Map<String, Object> createVnpayPaymentUrlWithData(ElevatorCardRegistrationDto dto, Long userId, HttpServletRequest request);

    String createVnpayPaymentUrl(Long registrationId, Long userId, HttpServletRequest request);

    void handleVnpayCallback(Long registrationId, Map<String, String> vnpParams);

    void cancelRegistration(Long registrationId, Long userId);
}

