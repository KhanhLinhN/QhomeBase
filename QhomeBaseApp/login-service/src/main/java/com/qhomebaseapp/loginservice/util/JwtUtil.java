package com.qhomebaseapp.loginservice.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    // Secret key, lưu trong application.properties hoặc environment biến tốt hơn
    private final String SECRET_KEY = "MySuperSecretKey12345";

    // Thời gian sống token: 24h
    private final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    // Tạo token
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // Lấy email từ token
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Kiểm tra token hợp lệ
    public boolean validateToken(String token, String email) {
        String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
