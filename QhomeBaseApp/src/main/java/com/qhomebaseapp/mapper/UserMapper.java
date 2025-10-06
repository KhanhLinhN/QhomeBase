package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.user.UserDto;
import com.qhomebaseapp.model.User;

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