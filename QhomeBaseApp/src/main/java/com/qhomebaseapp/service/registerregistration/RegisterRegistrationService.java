package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RegisterRegistrationService {

    RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId);

    List<RegisterServiceRequestResponseDto> getByUserId(Long userId);

    RegisterServiceRequestResponseDto updateRegistration(Long id, RegisterServiceRequestDto dto, Long userId);

    List<String> uploadVehicleImages(List<MultipartFile> files, Long userId);

    Page<RegisterServiceRequestResponseDto> getByUserIdPaginated(Long userId, int page, int size);

    String createVnpayPaymentUrl(Long registrationId, Long userId, HttpServletRequest request);

    void handleVnpayCallback(Long registrationId, Map<String, String> vnpParams);
}
