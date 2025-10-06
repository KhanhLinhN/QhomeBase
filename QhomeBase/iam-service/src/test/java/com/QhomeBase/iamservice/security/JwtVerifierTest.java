package com.QhomeBase.iamservice.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtVerifierTest {

    private JwtVerifier jwtVerifier;
    private JwtIssuer jwtIssuer;

    @BeforeEach
    void setUp() {
        String secret = "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation";
        jwtVerifier = new JwtVerifier(secret, "qhome-iam", "qhome-base");
        jwtIssuer = new JwtIssuer(secret, "qhome-iam", 15L);
    }

    @Test
    void testVerify_WithValidToken_ShouldReturnClaims() {
        // Given
        UUID uid = UUID.randomUUID();
        String username = "testuser";
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of("USER");
        List<String> perms = List.of("READ");
        String audience = "qhome-base";

        String token = jwtIssuer.issueForService(uid, username, tenantId, roles, perms, audience);

        // When
        Claims claims = jwtVerifier.verify(token);

        // Then
        assertNotNull(claims);
        assertEquals("qhome-iam", claims.getIssuer());
        assertEquals("qhome-base", claims.getAudience());
        assertEquals(username, claims.getSubject());
        assertEquals(uid.toString(), claims.get("uid"));
        assertEquals(tenantId.toString(), claims.get("tenant"));
        assertTrue(claims.get("roles") instanceof List);
        assertTrue(claims.get("perms") instanceof List);
    }

    @Test
    void testVerify_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtVerifier.verify(invalidToken);
        });
    }

    @Test
    void testVerify_WithWrongIssuer_ShouldThrowException() {
        // Given
        JwtIssuer wrongIssuer = new JwtIssuer(
            "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation",
            "wrong-issuer",
            15L
        );
        String token = wrongIssuer.issueForService(
            UUID.randomUUID(),
            "testuser",
            UUID.randomUUID(),
            List.of("USER"),
            List.of("READ"),
            "qhome-base"
        );

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtVerifier.verify(token);
        });
    }

    @Test
    void testVerify_WithWrongAudience_ShouldThrowException() {
        // Given
        String token = jwtIssuer.issueForService(
            UUID.randomUUID(),
            "testuser",
            UUID.randomUUID(),
            List.of("USER"),
            List.of("READ"),
            "wrong-audience"
        );

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtVerifier.verify(token);
        });
    }

    @Test
    void testVerify_WithExpiredToken_ShouldThrowException() {
        // Given - Tạo JwtIssuer với thời gian sống rất ngắn (0.001 minutes = 60ms)
        JwtIssuer shortLivedIssuer = new JwtIssuer(
            "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation",
            "qhome-iam",
            0L // 0 minutes = expired immediately
        );
        
        String token = shortLivedIssuer.issueForService(
            UUID.randomUUID(),
            "testuser",
            UUID.randomUUID(),
            List.of("USER"),
            List.of("READ"),
            "qhome-base"
        );

        // Wait a bit to ensure token is expired
        try {
            Thread.sleep(200); // Wait 200ms to ensure token is expired
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtVerifier.verify(token);
        });
    }

    @Test
    void testConstructor_WithShortSecret_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            new JwtVerifier("short", "qhome-iam", "qhome-base");
        });
    }
}
