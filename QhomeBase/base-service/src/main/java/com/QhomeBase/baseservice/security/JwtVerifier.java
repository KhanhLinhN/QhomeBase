package com.QhomeBase.baseservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
@Component
public class JwtVerifier {
    private final SecretKey key;
    private final String issuer;
    public JwtVerifier(@Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.issuer}") String issuer) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32)
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        this.key = Keys.hmacShaKeyFor(raw);
        this.issuer = issuer;
    }

    public Claims verify(String token) {
        return (Claims) Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();

    }
}
