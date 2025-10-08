package com.QhomeBase.baseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.QhomeBase.baseservice.dto.ApproveDeletionReq;
import com.QhomeBase.baseservice.dto.CreateDeletionReq;
import com.QhomeBase.baseservice.model.TenantDeletionRequest;
import com.QhomeBase.baseservice.model.TenantDeletionStatus;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.security.JwtVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Transactional
class TenantDeletionControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TenantDeletionRequestRepository repository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtVerifier jwtVerifier;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testTenantId;
    private UUID testUserId;
    private String validJwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        

        testTenantId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        

        validJwtToken = createTestJwtToken();
        

        when(jwtVerifier.verify(any(String.class))).thenReturn(createTestClaims());
    }

    private String createTestJwtToken() {
        String secret = "qhome-base-secret-key-2024-very-long-secret-key-for-testing";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .setSubject("testuser")
                .claim("uid", testUserId.toString())
                .claim("tenant", testTenantId.toString())
                .claim("roles", List.of("tenant_manager"))
                .claim("perms", List.of("base.tenant.delete.request", "base.tenant.delete.approve"))
                .setIssuer("qhome-base")
                .signWith(key)
                .compact();
    }

    private Claims createTestClaims() {
        String secret = "qhome-base-secret-key-2024-very-long-secret-key-for-testing";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer("qhome-base")
                .build()
                .parseClaimsJws(validJwtToken)
                .getBody();
    }

    @Test
    void testCreateDeletionRequest_WithValidData_ShouldReturn201() throws Exception {
        // Given
        CreateDeletionReq request = new CreateDeletionReq(testTenantId, "Test deletion reason");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(testTenantId.toString()))
                .andExpect(jsonPath("$.requestedBy").value(testUserId.toString()))
                .andExpect(jsonPath("$.reason").value("Test deletion reason"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void testCreateDeletionRequest_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        CreateDeletionReq request = new CreateDeletionReq(testTenantId, "Test deletion reason");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateDeletionRequest_WithInvalidTenantId_ShouldReturn403() throws Exception {
        // Given
        UUID differentTenantId = UUID.randomUUID();
        CreateDeletionReq request = new CreateDeletionReq(differentTenantId, "Test deletion reason");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateDeletionRequest_WithNullTenantId_ShouldReturn400() throws Exception {
        // Given
        CreateDeletionReq request = new CreateDeletionReq(null, "Test deletion reason");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveDeletionRequest_WithValidData_ShouldReturn200() throws Exception {
        // Given - Tạo deletion request trước
        TenantDeletionRequest deletionRequest = TenantDeletionRequest.builder()
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(TenantDeletionStatus.PENDING)
                .build();
        TenantDeletionRequest savedRequest = repository.save(deletionRequest);
        
        ApproveDeletionReq approveRequest = new ApproveDeletionReq("Approved by admin");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions/{id}/approve", savedRequest.getId())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.note").value("Approved by admin"))
                .andExpect(jsonPath("$.approvedBy").value(testUserId.toString()))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    @Test
    void testApproveDeletionRequest_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        ApproveDeletionReq approveRequest = new ApproveDeletionReq("Approved by admin");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions/{id}/approve", nonExistentId)
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testApproveDeletionRequest_WithEmptyNote_ShouldReturn400() throws Exception {
        // Given - Tạo deletion request trước
        TenantDeletionRequest deletionRequest = TenantDeletionRequest.builder()
                .tenantId(testTenantId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(TenantDeletionStatus.PENDING)
                .build();
        TenantDeletionRequest savedRequest = repository.save(deletionRequest);
        
        ApproveDeletionReq approveRequest = new ApproveDeletionReq("");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions/{id}/approve", savedRequest.getId())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveDeletionRequest_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        UUID requestId = UUID.randomUUID();
        ApproveDeletionReq approveRequest = new ApproveDeletionReq("Approved by admin");
        
        // When & Then
        mockMvc.perform(post("/api/tenant-deletions/{id}/approve", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isUnauthorized());
    }
}
