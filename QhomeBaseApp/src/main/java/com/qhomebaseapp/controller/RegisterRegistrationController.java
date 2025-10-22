package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
public class RegisterRegistrationController {

    private final RegisterRegistrationService service;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegisterServiceRequestResponseDto> register(
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        RegisterServiceRequestResponseDto result = service.registerService(dto, userId);

        log.info("User {} registered service {}", userId, dto.getServiceType());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RegisterServiceRequestResponseDto>> getByUser(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<RegisterServiceRequestResponseDto> list = service.getByUserId(userId);
        log.info("User {} fetched their registered services, count={}", userId, list.size());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RegisterServiceRequestResponseDto> updateRegistration(
            @PathVariable Long id,
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        RegisterServiceRequestResponseDto result = service.updateRegistration(id, dto, userId);

        log.info("User {} updated registration {}", userId, id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload-images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        List<String> imageUrls = service.uploadVehicleImages(files, userId);
        return ResponseEntity.ok(Map.of("imageUrls", imageUrls));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUserId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in authentication");
    }

    @GetMapping("/me/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByUserPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getUserIdFromAuthentication(authentication);

        // Spring Pageable index bắt đầu từ 0
        int pageIndex = page > 0 ? page - 1 : 0;

        Page<RegisterServiceRequestResponseDto> result = service.getByUserIdPaginated(userId, pageIndex, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Danh sách thẻ xe đã đăng ký",
                "data", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "currentPage", page
        ));
    }

}
