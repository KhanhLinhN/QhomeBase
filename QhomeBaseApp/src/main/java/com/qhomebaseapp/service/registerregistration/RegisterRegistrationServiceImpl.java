package com.qhomebaseapp.service.registerregistration;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.mapper.RegisterServiceRequestMapper;
import com.qhomebaseapp.mapper.RegisterServiceRequestResponseMapper;
import com.qhomebaseapp.model.RegisterServiceImage;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.registerregistration.RegisterRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterRegistrationServiceImpl implements RegisterRegistrationService {

    private final RegisterRegistrationRepository repository;
    private final UserRepository userRepository;
    private final RegisterServiceRequestMapper registerServiceRequestMapper;
    private final RegisterServiceRequestResponseMapper registerServiceRequestResponseMapper;

    @Override
    public RegisterServiceRequestResponseDto registerService(RegisterServiceRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        RegisterServiceRequest entity = registerServiceRequestMapper.toEntity(dto);
        entity.setUser(user);
        entity.setStatus("PENDING");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            dto.getImageUrls().forEach(url -> {
                RegisterServiceImage image = RegisterServiceImage.builder()
                        .registerServiceRequest(entity)
                        .imageUrl(url)
                        .createdAt(OffsetDateTime.now())
                        .build();
                entity.getImages().add(image);
            });
        }

        RegisterServiceRequest saved = repository.save(entity);
        return registerServiceRequestResponseMapper.toDto(saved);
    }


    @Override
    public List<RegisterServiceRequestResponseDto> getByUserId(Long userId) {
        List<RegisterServiceRequest> list = repository.findByUser_Id(userId);
        return list.stream()
                .map(registerServiceRequestResponseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RegisterServiceRequestResponseDto updateRegistration(Long id, RegisterServiceRequestDto dto, Long userId) {
        RegisterServiceRequest entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify others’ registration");
        }

        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getVehicleBrand() != null) entity.setVehicleBrand(dto.getVehicleBrand());
        if (dto.getVehicleColor() != null) entity.setVehicleColor(dto.getVehicleColor());
        if (dto.getLicensePlate() != null) entity.setLicensePlate(dto.getLicensePlate());
        if (dto.getVehicleType() != null) entity.setVehicleType(dto.getVehicleType());

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {

            entity.getImages().clear();

            dto.getImageUrls().forEach(url -> {
                RegisterServiceImage image = RegisterServiceImage.builder()
                        .registerServiceRequest(entity)
                        .imageUrl(url)
                        .createdAt(OffsetDateTime.now())
                        .build();
                entity.getImages().add(image);
            });
        }

        entity.setUpdatedAt(OffsetDateTime.now());
        RegisterServiceRequest updated = repository.save(entity);

        return registerServiceRequestResponseMapper.toDto(updated);
    }


    @Override
    public List<String> uploadVehicleImages(List<MultipartFile> files, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files uploaded");
        }

        try {
            String uploadDir = "uploads/vehicles/";
            Files.createDirectories(Path.of(uploadDir));

            List<String> urls = files.stream().map(file -> {
                String fileName = "vehicle_" + userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = Path.of(uploadDir + fileName);
                try {
                    file.transferTo(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Error saving file: " + fileName);
                }
                return "/uploads/vehicles/" + fileName;
            }).collect(Collectors.toList());

            log.info("User {} uploaded {} vehicle images", userId, urls.size());
            return urls;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading images: " + e.getMessage());
        }
    }

    @Override
    public Page<RegisterServiceRequestResponseDto> getByUserIdPaginated(Long userId, int page, int size) {
        Page<RegisterServiceRequest> pageResult =
                repository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return pageResult.map(registerServiceRequestResponseMapper::toDto);
    }
}
