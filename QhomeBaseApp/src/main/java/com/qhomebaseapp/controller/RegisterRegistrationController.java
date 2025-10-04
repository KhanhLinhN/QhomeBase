package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/register-service")
@RequiredArgsConstructor
public class RegisterRegistrationController {

    private final RegisterRegistrationService service;

    @PostMapping
    public ResponseEntity<RegisterServiceRequest> register(@RequestBody RegisterServiceRequestDto dto) {
        RegisterServiceRequest result = service.registerService(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<RegisterServiceRequest>> getByUser(@PathVariable Long userId) {
        List<RegisterServiceRequest> list = service.getByUserId(userId);
        return ResponseEntity.ok(list);
    }
}
