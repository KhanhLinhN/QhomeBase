package com.qhomebaseapp.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    @NotBlank
    private String fullName;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
    private String gender;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^[0-9]{9,12}$", message = "Invalid phone number")
    private String phoneNumber;

    private String avatarUrl;

    private String apartmentName;
    private String buildingBlock;
    private Integer floorNumber;
    private String unitId; // UUID của căn hộ từ admin system
    private String address;
    private String citizenId;
}