package com.qhomebaseapp.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String accessToken;
    private String refreshToken;

    private Long userId;
    private String username;
    private String role;

    @Builder.Default
    private String tokenType = "Bearer";
}
