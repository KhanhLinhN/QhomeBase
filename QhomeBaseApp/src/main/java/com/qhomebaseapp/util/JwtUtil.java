package com.qhomebaseapp.util;

import io.jsonwebtoken.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.qhomebaseapp.model.User;

import java.security.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    private final JwtKeyService keyService;
    private final RedisTemplate<String, String> redisTemplate;

    private final long accessExpirationMs = 15 * 60 * 1000L;
    private final long refreshExpirationMs = 30L * 24 * 60 * 60 * 1000L;

    private static final String ISSUER = "qhomebase-api";
    private static final String AUDIENCE = "qhomebase-client";

    public JwtUtil(JwtKeyService keyService, RedisTemplate<String, String> redisTemplate) {
        this.keyService = keyService;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String kid = keyService.getCurrentKid();

        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setId(jti)
                .setSubject(user.getUsername())
                .claim("roles", List.of("ROLE_" + user.getRole()))
                .claim("token_type", "access")
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(keyService.getPrivateKey(kid), SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String kid = keyService.getCurrentKid();

        return Jwts.builder()
                .setHeaderParam("kid", kid)
                .setId(jti)
                .setSubject(username)
                .claim("token_type", "refresh")
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(refreshExpirationMs)))
                .signWith(keyService.getPrivateKey(kid), SignatureAlgorithm.RS256)
                .compact();
    }

    public void blacklistAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String jti = claims.getId();
            Date exp = claims.getExpiration();
            if (jti != null && exp != null) {
                long ttl = Math.max(1, (exp.getTime() - System.currentTimeMillis()) / 1000);
                redisTemplate.opsForValue().set("blacklist:" + jti, "1", ttl, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {}
    }

    private boolean isTokenBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey("blacklist:" + jti);
        return Boolean.TRUE.equals(exists);
    }

    public String extractJti(String token) {
        try {
            return parseClaims(token).getId();
        } catch (JwtException e) {
            return null;
        }
    }

    private String extractKidFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            int kidStart = headerJson.indexOf("\"kid\"");
            if (kidStart == -1) return null;
            int colon = headerJson.indexOf(":", kidStart);
            int firstQuote = headerJson.indexOf("\"", colon);
            int secondQuote = headerJson.indexOf("\"", firstQuote + 1);
            return headerJson.substring(firstQuote + 1, secondQuote);
        } catch (Exception e) {
            return null;
        }
    }

    public Claims parseClaims(String token) {
        String kid = extractKidFromToken(token);
        PublicKey pub = (kid == null)
                ? keyService.getCurrentPublicKey()
                : keyService.getPublicKeyByKid(kid);

        return Jwts.parserBuilder()
                .setSigningKey(pub)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token, String username, String expectedType) {
        try {
            Claims claims = parseClaims(token);

            if (!ISSUER.equals(claims.getIssuer())) return false;
            if (!AUDIENCE.equals(claims.getAudience())) return false;
            if (!username.equals(claims.getSubject())) return false;
            if (!expectedType.equalsIgnoreCase(claims.get("token_type", String.class))) return false;
            if (claims.getExpiration().before(new Date())) return false;
            if (isTokenBlacklisted(claims.getId())) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
