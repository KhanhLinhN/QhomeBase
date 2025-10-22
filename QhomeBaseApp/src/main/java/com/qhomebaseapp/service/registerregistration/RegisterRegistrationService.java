package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RegisterRegistrationService {

    RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId);

    List<RegisterServiceRequestResponseDto> getByUserId(Long userId);

    RegisterServiceRequestResponseDto updateRegistration(Long id, RegisterServiceRequestDto dto, Long userId);

    List<String> uploadVehicleImages(List<MultipartFile> files, Long userId);

    Page<RegisterServiceRequestResponseDto> getByUserIdPaginated(Long userId, int page, int size);
}
