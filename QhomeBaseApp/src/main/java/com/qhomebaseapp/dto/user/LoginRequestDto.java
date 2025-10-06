package com.qhomebaseapp.dto.user;


import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String password;
}