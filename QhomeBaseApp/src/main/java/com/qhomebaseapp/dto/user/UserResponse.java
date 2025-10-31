package com.qhomebaseapp.dto.user;

import com.qhomebaseapp.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String token;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String gender;
    private String address;
    private String apartmentName;
    private String buildingBlock;
    private Integer floorNumber;
    private String unitId; // UUID của căn hộ từ admin system
    private String citizenId;
    private LocalDate dateOfBirth;
    
    public static UserResponse fromEntity(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setGender(user.getGender());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setAddress(user.getAddress());
        dto.setApartmentName(user.getApartmentName());
        dto.setBuildingBlock(user.getBuildingBlock());
        dto.setFloorNumber(user.getFloorNumber());
        dto.setUnitId(user.getUnitId());
        dto.setCitizenId(user.getCitizenId());
        return dto;
    }
}


