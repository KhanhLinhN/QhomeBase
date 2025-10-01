package com.qhomebaseapp.loginservice.mapper;

import com.qhomebaseapp.loginservice.dto.UserDto;
import com.qhomebaseapp.loginservice.entity.User;

public class UserMapper {
    public static User toEntity(UserDto dto) {
        return User.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .email(dto.getEmail())
                .build();
    }

    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setEmail(user.getEmail());
        return dto;
    }
}