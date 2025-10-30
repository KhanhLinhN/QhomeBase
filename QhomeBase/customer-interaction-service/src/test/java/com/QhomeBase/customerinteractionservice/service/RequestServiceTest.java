package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private requestRepository requestRepository;

    @Mock
    private processingLogRepository processingLogRepository;

    @InjectMocks
    private requestService requestService;

    private Request mockRequest;
    private RequestDTO mockRequestDTO;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        mockRequest = new Request();
        mockRequest.setId(requestId);
        mockRequest.setRequestCode("RE00001");
        mockRequest.setTitle("Fix leaking pipe");
        mockRequest.setResidentName("Nguyen Khanh Linh");
        mockRequest.setStatus("New");
        mockRequest.setPriority("High");
        mockRequest.setTenantId(UUID.randomUUID());
        mockRequest.setCreatedAt(now);
        mockRequest.setUpdatedAt(now);

        mockRequestDTO = new RequestDTO(
                requestId, "RE00001", mockRequest.getTenantId(), UUID.randomUUID(),
                "John Doe", null, "Fix leaking pipe", "Water is leaking in the kitchen.",
                "New", "High", now.toString().replace("T", " "), now.toString().replace("T", " ")
        );
    }

    @Test
    @DisplayName("Test getRequestById -  When founded Request")
    void getRequestById_whenRequestFound_shouldReturnRequestDTO() {

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(mockRequest));

        RequestDTO foundDto = requestService.getRequestById(requestId);

        assertNotNull(foundDto);
        assertEquals(requestId, foundDto.getId());
        assertEquals("Fix leaking pipe", foundDto.getTitle());
        verify(requestRepository, times(1)).findById(requestId);
    }

    @Test
    @DisplayName("Test getRequestById - When cannot find Request")
    void getRequestById_whenRequestNotFound_shouldThrowException() {

        UUID nonExistentId = UUID.randomUUID();

        when(requestRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            requestService.getRequestById(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("Request not found with id: " + nonExistentId));
        verify(requestRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("Test createNewRequest - Create new succesfully")
    void createNewRequest_shouldSaveRequestAndLog_andReturnDTO() {
        // Arrange
        when(requestRepository.save(any(Request.class))).thenReturn(mockRequest);

        when(processingLogRepository.save(any(ProcessingLog.class))).thenReturn(new ProcessingLog());

        ArgumentCaptor<ProcessingLog> logCaptor = ArgumentCaptor.forClass(ProcessingLog.class);

        RequestDTO createdDto = requestService.createNewRequest(mockRequestDTO);

        assertNotNull(createdDto);
        assertEquals(mockRequest.getRequestCode(), createdDto.getRequestCode());

        verify(requestRepository, times(1)).save(any(Request.class));
        verify(processingLogRepository, times(1)).save(logCaptor.capture());

        ProcessingLog capturedLog = logCaptor.getValue();
        assertEquals("Request", capturedLog.getRecordType());
        assertEquals(mockRequest.getId(), capturedLog.getRecordId());
        assertEquals("New", capturedLog.getRequestStatus());
        assertTrue(capturedLog.getContent().contains("Request created by: Nguyen Khanh Linh"));
    }

    @Test
    @DisplayName("Test getFilteredRequests - Get list successfull")
    void getFilteredRequests_shouldReturnPagedDTOs() {
        // Arrange
        List<Request> requestList = Arrays.asList(mockRequest);
        Page<Request> requestPage = new PageImpl<>(requestList);

        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(requestPage);

        // Act
        Page<RequestDTO> resultPage = requestService.getFilteredRequests(
                "REQ", "pipe", "Linh", null, "New", "High", 0, null, null
        );

        // Assert
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals("RE00001", resultPage.getContent().get(0).getRequestCode());
        verify(requestRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Test getRequestCounts - Count request by status")
    void getRequestCounts_shouldReturnMapWithCountsAndTotal() {
        // Arrange
        List<StatusCountDTO> counts = Arrays.asList(
                new StatusCountDTO("New", 5L),
                new StatusCountDTO("Completed", 10L)
        );
        when(requestRepository.countRequestsByStatus(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(counts);

        // Act
        Map<String, Long> result = requestService.getRequestCounts(
                null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.get("New"));
        assertEquals(10L, result.get("Completed"));
        assertEquals(15L, result.get("total")); // 5 + 10 = 15
        verify(requestRepository, times(1)).countRequestsByStatus(any(), any(), any(), any(), any(), any(), any());
    }
}