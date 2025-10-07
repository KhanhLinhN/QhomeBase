package com.qhomebaseapp.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.qhomebaseapp.model.User;

import java.security.*;
import java.security.spec.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.private-key-pem}")
    private String PRIVATE_KEY_PEM;

    @Value("${jwt.public-key-pem}")
    private String PUBLIC_KEY_PEM;

    @Value("${jwt.access.expiration:900000}") // 15 phút
    private long accessExpirationMs;

    @Value("${jwt.refresh.expiration:1209600000}") // 14 ngày
    private long refreshExpirationMs;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    // --- Key caching ---
    private synchronized PrivateKey getPrivateKey() {
        if (privateKey == null) {
            privateKey = parsePrivateKeyFromPem(PRIVATE_KEY_PEM);
        }
        return privateKey;
    }

    private synchronized PublicKey getPublicKey() {
        if (publicKey == null) {
            publicKey = parsePublicKeyFromPem(PUBLIC_KEY_PEM);
        }
        return publicKey;
    }

    // --- Generate token ---
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of("ROLE_" + user.getRole()));
        claims.put("token_type", "access");

        return buildToken(claims, user.getUsername(), accessExpirationMs);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        claims.put("token_type", "access");

        return buildToken(claims, userDetails.getUsername(), accessExpirationMs);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        return buildToken(claims, username, refreshExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer("qhomebase-api")
                .setAudience("qhomebase-client")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ttlMillis)))
                .setId(UUID.randomUUID().toString())
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    // --- Extract / Parse ---
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .setAllowedClockSkewSeconds(10)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT: " + e.getMessage(), e);
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        try {
            return extractClaim(token, c -> c.get("token_type", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isAccessToken(String token) {
        return "access".equalsIgnoreCase(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equalsIgnoreCase(extractTokenType(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            String type = claims.get("token_type", String.class);
            if (username == null || !username.equals(userDetails.getUsername())) return false;
            if (type == null || !type.equals(expectedType)) return false;
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // --- PEM Key parsing ---
    private PrivateKey parsePrivateKeyFromPem(String pem) {
        if (pem == null || pem.isBlank()) throw new IllegalStateException("Private key PEM not configured");
        try {
            String cleaned = pem.replaceAll("-----BEGIN ([A-Z ]+)-----", "")
                    .replaceAll("-----END ([A-Z ]+)-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    private PublicKey parsePublicKeyFromPem(String pem) {
        if (pem == null || pem.isBlank()) throw new IllegalStateException("Public key PEM not configured");
        try {
            String cleaned = pem.replaceAll("-----BEGIN ([A-Z ]+)-----", "")
                    .replaceAll("-----END ([A-Z ]+)-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key", e);
        }
    }
}
