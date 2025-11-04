package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.user.UpdateProfileRequest;
import com.qhomebaseapp.dto.user.UserResponse;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getApartmentName() != null) user.setApartmentName(request.getApartmentName());
        if (request.getBuildingBlock() != null) user.setBuildingBlock(request.getBuildingBlock());
        if (request.getFloorNumber() != null) user.setFloorNumber(request.getFloorNumber());
        if (request.getUnitId() != null && !request.getUnitId().isBlank()) {
            user.setUnitId(request.getUnitId());
        }
        if (request.getResidentId() != null && !request.getResidentId().isBlank()) {
            user.setResidentId(request.getResidentId());
        }
        if (request.getBuildingId() != null && !request.getBuildingId().isBlank()) {
            user.setBuildingId(request.getBuildingId());
        }
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCitizenId() != null) user.setCitizenId(request.getCitizenId());

        userRepository.save(user);

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            String uploadDir = "uploads/avatar/";
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(uploadDir));

            String fileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path filePath = java.nio.file.Path.of(uploadDir + fileName);

            file.transferTo(filePath);

            String avatarUrl = "/uploads/avatar/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error uploading file: " + e.getMessage()));
        }
    }
}
