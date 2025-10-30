package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.service.requestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private requestService requestService;

    @InjectMocks
    private requestController requestController;

    private RequestDTO sampleRequestDTO;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleRequestDTO = RequestDTO.builder()
                .id(sampleId)
                .requestCode("RE00001")
                .residentId(UUID.randomUUID())
                .residentName("Nguyen Khanh Linh")
                .title("Test Request")
                .status("New")
                .priority("High")
                .createdAt("2025-10-15 10:00:00")
                .updatedAt("2025-10-15 10:00:00")
                .build();
    }

    @Test
    @DisplayName("GET /requests")
    void getRequestsList_shouldReturnPageOfDTOs() {
        // Arrange
        Page<RequestDTO> requestPage = new PageImpl<>(Collections.singletonList(sampleRequestDTO));
        when(requestService.getFilteredRequests(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(requestPage);

        Page<RequestDTO> result = requestController.getRequestsList(
                null, null, null, null, null, null, 0, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleId, result.getContent().get(0).getId());
        verify(requestService, times(1)).getFilteredRequests(
                any(), any(), any(), any(), any(), any(), anyInt(), any(), any()
        );
    }

    @Test
    @DisplayName("GET /requests/counts ")
    void getRequestCounts_shouldReturnCountMap() {
        // Arrange
        Map<String, Long> expectedCounts = new HashMap<>();
        expectedCounts.put("New", 5L);
        expectedCounts.put("total", 5L);
        when(requestService.getRequestCounts(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedCounts);

        Map<String, Long> result = requestController.getRequestCounts(
                null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.get("New"));
        assertEquals(5L, result.get("total"));
    }

    @Test
    @DisplayName("GET /requests/{id} ")
    void getRequest_whenIdIsValid_shouldReturnDTO() {

        when(requestService.getRequestById(sampleId)).thenReturn(sampleRequestDTO);

        RequestDTO result = requestController.getRequest(sampleId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleId, result.getId());
        assertEquals("Test Request", result.getTitle());
        verify(requestService, times(1)).getRequestById(sampleId);
    }

    @Test
    @DisplayName("GET /requests/{id} ")
    void getRequest_whenIdIsInvalid_shouldThrowException() {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(requestService.getRequestById(invalidId))
                .thenThrow(new RuntimeException("Request not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> requestController.getRequest(invalidId));
        verify(requestService, times(1)).getRequestById(invalidId);
    }

    @Test
    @DisplayName("POST /createRequest - success ")
    void addNewRequest_whenValidDTO_shouldReturnCreatedWithSavedDTO() {
        // Arrange
        when(requestService.createNewRequest(any(RequestDTO.class))).thenReturn(sampleRequestDTO);

        // Act
        ResponseEntity<RequestDTO> response = requestController.addNewRequest(sampleRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sampleRequestDTO.getId(), response.getBody().getId());
        verify(requestService, times(1)).createNewRequest(sampleRequestDTO);
    }
}