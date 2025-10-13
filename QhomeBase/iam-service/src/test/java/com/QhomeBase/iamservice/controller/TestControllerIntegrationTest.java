package com.QhomeBase.iamservice.controller;

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
class TestControllerIntegrationTest {

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
    void testPublicEndpoint_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a public endpoint - no JWT required"));
    }

    @Test
    void testProtectedEndpoint_WithoutToken_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a protected endpoint - JWT required"));
    }

    @Test
    void testGenerateToken_WithValidRequest_ShouldReturn200() throws Exception {
        TestController.TokenRequest request = new TestController.TokenRequest();
        request.setUid(UUID.randomUUID());
        request.setUsername("testuser");
        request.setTenantId(UUID.randomUUID());
        request.setRoles(List.of("USER", "ADMIN"));
        request.setPermissions(List.of("READ", "WRITE"));

        mockMvc.perform(post("/api/test/generate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Token generated successfully"));
    }

    @Test
    void testGenerateToken_WithEmptyRequest_ShouldReturn200() throws Exception {
        TestController.TokenRequest request = new TestController.TokenRequest();

        mockMvc.perform(post("/api/test/generate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Token generated successfully"));
    }

    @Test
    void testGenerateToken_WithInvalidJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/test/generate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }
}










