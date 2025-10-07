package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.mapper.RegisterServiceRequestMapper;
import com.qhomebaseapp.mapper.RegisterServiceRequestResponseMapper;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.registerregistration.RegisterRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegisterRegistrationServiceImpl implements RegisterRegistrationService {

    private final RegisterRegistrationRepository repository;
    private final UserRepository userRepository;

    @Override
    public RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        RegisterServiceRequest entity = RegisterServiceRequestMapper.INSTANCE.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        RegisterServiceRequest saved = repository.save(entity);
        return RegisterServiceRequestResponseMapper.INSTANCE.toDto(saved);
    }

    @Override
    public List<RegisterServiceRequestResponseDto> getByUserId(Long userId) {
        List<RegisterServiceRequest> list = repository.findByUser_Id(userId);
        return list.stream()
                .map(RegisterServiceRequestResponseMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }
}
