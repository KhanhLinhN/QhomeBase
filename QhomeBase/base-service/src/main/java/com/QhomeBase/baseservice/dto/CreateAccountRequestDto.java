package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAccountRequestDto(
        UUID residentId,
        
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscore, and hyphen")
        String username,
        
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,
        
        boolean autoGenerate,
        
        String proofOfRelationImageUrl
) {
    public CreateAccountRequestDto {
        if (!autoGenerate) {
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username is required when autoGenerate is false");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required when autoGenerate is false");
            }
        }
    }
}

