package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.mapper.RegisterServiceRequestMapper;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.registerregistration.RegisterRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterRegistrationServiceImpl implements RegisterRegistrationService {

    private final RegisterRegistrationRepository repository;
    private final UserRepository userRepository;

    @Override
    public RegisterServiceRequest registerService(RegisterServiceRequestDto dto) {
        // ✅ Lấy user từ DB
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getUserId()));

        // ✅ Tạo entity mới
        RegisterServiceRequest entity = RegisterServiceRequestMapper.INSTANCE.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        return repository.save(entity);
    }

    @Override
    public List<RegisterServiceRequest> getByUserId(Long userId) {
        return repository.findByUserId(userId);
    }
}
