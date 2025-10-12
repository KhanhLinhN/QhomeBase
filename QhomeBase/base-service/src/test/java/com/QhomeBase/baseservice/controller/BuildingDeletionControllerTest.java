package com.QhomeBase.baseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.security.JwtVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class BuildingDeletionControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BuildingDeletionRequestRepository repository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private JwtVerifier jwtVerifier;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testTenantId;
    private UUID testBuildingId;
    private UUID testUserId;
    private String validJwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();

        testTenantId = UUID.randomUUID();
        testBuildingId = UUID.randomUUID();
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
                .claim("perms", List.of("base.building.delete.request", "base.building.delete.approve"))
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
    void testCreateDeletionRequest_WithValidData_ShouldReturn200() throws Exception {
        // Given
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        // When & Then
        mockMvc.perform(post("/api/delete-buildings")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(testTenantId.toString()))
                .andExpect(jsonPath("$.buildingId").value(testBuildingId.toString()))
                .andExpect(jsonPath("$.requestedBy").value(testUserId.toString()))
                .andExpect(jsonPath("$.reason").value("Test deletion reason"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void testCreateDeletionRequest_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(
                testTenantId, testBuildingId, "Test deletion reason"
        );

        // When & Then
        mockMvc.perform(post("/api/delete-buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateDeletionRequest_WithInvalidTenantId_ShouldReturn403() throws Exception {
        // Given
        UUID differentTenantId = UUID.randomUUID();
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(
                differentTenantId, testBuildingId, "Test deletion reason"
        );

        // When & Then
        mockMvc.perform(post("/api/delete-buildings")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateDeletionRequest_WithNullTenantId_ShouldReturn400() throws Exception {
        // Given
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(
                null, testBuildingId, "Test deletion reason"
        );

        // When & Then
        mockMvc.perform(post("/api/delete-buildings")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateDeletionRequest_WithNullBuildingId_ShouldReturn400() throws Exception {
        // Given
        BuildingDeletionCreateReq request = new BuildingDeletionCreateReq(
                testTenantId, null, "Test deletion reason"
        );

        // When & Then
        mockMvc.perform(post("/api/delete-buildings")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveDeletionRequest_WithValidData_ShouldReturn200() throws Exception {
        // Given - Tạo deletion request trước
        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .build();
        BuildingDeletionRequest savedRequest = repository.save(deletionRequest);

        BuildingDeletionApproveReq approveRequest = new BuildingDeletionApproveReq("Approved by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/approve")
                .param("requestId", savedRequest.getId().toString())
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
    void testApproveDeletionRequest_WithNonExistentId_ShouldReturn500() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        BuildingDeletionApproveReq approveRequest = new BuildingDeletionApproveReq("Approved by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/approve")
                .param("requestId", nonExistentId.toString())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testApproveDeletionRequest_WithEmptyNote_ShouldReturn400() throws Exception {
        // Given - Tạo deletion request trước
        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .build();
        BuildingDeletionRequest savedRequest = repository.save(deletionRequest);

        BuildingDeletionApproveReq approveRequest = new BuildingDeletionApproveReq("");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/approve")
                .param("requestId", savedRequest.getId().toString())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveDeletionRequest_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        UUID requestId = UUID.randomUUID();
        BuildingDeletionApproveReq approveRequest = new BuildingDeletionApproveReq("Approved by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/approve")
                .param("requestId", requestId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRejectDeletionRequest_WithValidData_ShouldReturn200() throws Exception {
        // Given - Tạo deletion request trước
        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .build();
        BuildingDeletionRequest savedRequest = repository.save(deletionRequest);

        BuildingDeletionRejectReq rejectRequest = new BuildingDeletionRejectReq("Rejected by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/{id}/reject", savedRequest.getId())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.note").value("Rejected by admin"))
                .andExpect(jsonPath("$.approvedBy").value(testUserId.toString()))
                .andExpect(jsonPath("$.approvedAt").isNotEmpty());
    }

    @Test
    void testRejectDeletionRequest_WithNonExistentId_ShouldReturn500() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        BuildingDeletionRejectReq rejectRequest = new BuildingDeletionRejectReq("Rejected by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/{id}/reject", nonExistentId)
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testRejectDeletionRequest_WithEmptyNote_ShouldReturn400() throws Exception {
        // Given - Tạo deletion request trước
        BuildingDeletionRequest deletionRequest = BuildingDeletionRequest.builder()
                .tenantId(testTenantId)
                .buildingId(testBuildingId)
                .requestedBy(testUserId)
                .reason("Test deletion reason")
                .status(BuildingDeletionStatus.PENDING)
                .build();
        BuildingDeletionRequest savedRequest = repository.save(deletionRequest);

        BuildingDeletionRejectReq rejectRequest = new BuildingDeletionRejectReq("");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/{id}/reject", savedRequest.getId())
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRejectDeletionRequest_WithoutToken_ShouldReturn401() throws Exception {
        // Given
        UUID requestId = UUID.randomUUID();
        BuildingDeletionRejectReq rejectRequest = new BuildingDeletionRejectReq("Rejected by admin");

        // When & Then
        mockMvc.perform(post("/api/delete-buildings/{id}/reject", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isUnauthorized());
    }
}
