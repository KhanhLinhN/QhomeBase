package com.QhomeBase.iamservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserForResidentDto(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        
        @Email(message = "Email must be valid")
        String email,
        
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,
        
        boolean autoGenerate,
        
        UUID residentId
) {}

