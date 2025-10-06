package com.QhomeBase.iamservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtIssuerTest {

    private JwtIssuer jwtIssuer;

    @BeforeEach
    void setUp() {
        jwtIssuer = new JwtIssuer(
            "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation",
            "qhome-iam",
            15L
        );
    }

    @Test
    void testIssueForService_ShouldGenerateValidToken() {
        // Given
        UUID uid = UUID.randomUUID();
        String username = "testuser";
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of("USER", "ADMIN");
        List<String> perms = List.of("READ", "WRITE");
        String audience = "qhome-base";

        // When
        String token = jwtIssuer.issueForService(uid, username, tenantId, roles, perms, audience);

        // Then
        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void testIssueForService_WithEmptyRolesAndPermissions() {
        // Given
        UUID uid = UUID.randomUUID();
        String username = "testuser";
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of();
        List<String> perms = List.of();
        String audience = "qhome-base";

        // When
        String token = jwtIssuer.issueForService(uid, username, tenantId, roles, perms, audience);

        // Then
        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    void testIssueForService_WithNullValues() {
        // Given
        UUID uid = UUID.randomUUID();
        String username = "testuser";
        UUID tenantId = UUID.randomUUID();
        List<String> roles = null;
        List<String> perms = null;
        String audience = "qhome-base";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtIssuer.issueForService(uid, username, tenantId, roles, perms, audience);
        });
    }

    @Test
    void testConstructor_WithShortSecret_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            new JwtIssuer("short", "qhome-iam", 15L);
        });
    }

    @Test
    void testConstructor_WithValidSecret_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> {
            new JwtIssuer(
                "qhome-iam-secret-key-2024-very-long-and-secure-key-for-jwt-token-generation",
                "qhome-iam",
                15L
            );
        });
    }
}

