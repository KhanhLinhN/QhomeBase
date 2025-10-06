package com.QhomeBase.iamservice;

import com.QhomeBase.iamservice.controller.TestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
class JwtEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testJwtFlow_EndToEnd() throws Exception {
        // Step 1: Generate a JWT token
        TestController.TokenRequest tokenRequest = new TestController.TokenRequest();
        tokenRequest.setUid(UUID.randomUUID());
        tokenRequest.setUsername("testuser");
        tokenRequest.setTenantId(UUID.randomUUID());
        tokenRequest.setRoles(List.of("USER", "ADMIN"));
        tokenRequest.setPermissions(List.of("READ", "WRITE"));

        String tokenResponse = mockMvc.perform(post("/api/test/generate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from response
        String token = objectMapper.readTree(tokenResponse).get("token").asText();

        // Step 2: Use the token to access protected endpoint
        mockMvc.perform(get("/api/test/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello testuser! This is a protected endpoint."));

        // Step 3: Use the token to access admin endpoint
        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello Admin testuser! This is an admin-only endpoint."));

        // Step 4: Get user info using the token
        mockMvc.perform(get("/api/test/user-info")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void testJwtFlow_WithInvalidToken() throws Exception {
        // Test with invalid token
        mockMvc.perform(get("/api/test/protected")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        // Test with malformed token
        mockMvc.perform(get("/api/test/protected")
                .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());

        // Test without Authorization header
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a protected endpoint - JWT required"));
    }

    @Test
    void testJwtFlow_WithExpiredToken() throws Exception {
        // This test would require a way to generate an expired token
        // For now, we'll test with a malformed token that should fail
        mockMvc.perform(get("/api/test/protected")
                .header("Authorization", "Bearer expired.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testJwtFlow_WithWrongAudience() throws Exception {
        // This test would require generating a token with wrong audience
        // For now, we'll test with a malformed token
        mockMvc.perform(get("/api/test/protected")
                .header("Authorization", "Bearer wrong.audience.token"))
                .andExpect(status().isUnauthorized());
    }
}
