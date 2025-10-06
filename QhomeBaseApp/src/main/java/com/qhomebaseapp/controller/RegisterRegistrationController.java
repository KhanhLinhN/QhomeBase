package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import com.qhomebaseapp.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
public class RegisterRegistrationController {

    private final RegisterRegistrationService service;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RegisterServiceRequest> register(
            @RequestBody RegisterServiceRequestDto dto,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RegisterServiceRequest result = service.registerService(dto, userId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<List<RegisterServiceRequest>> getByUser(Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<RegisterServiceRequest> list = service.getByUserId(userId);
        return ResponseEntity.ok(list);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return null;
        }

        String userEmail = authentication.getName();

        return userService.getUserByEmail(userEmail)
                .map(user -> user.getId().longValue())
                .orElse(null);
    }
}
