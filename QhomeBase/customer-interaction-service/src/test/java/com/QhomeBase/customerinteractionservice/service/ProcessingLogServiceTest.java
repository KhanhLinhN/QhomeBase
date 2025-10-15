package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingLogServiceTest {

    @Mock
    private processingLogRepository processingLogRepository;

    @Mock
    private requestRepository requestRepository;

    @InjectMocks
    private processingLogService processingLogService;

    private UUID recordId;
    private UUID staffId;
    private Request mockRequest;
    private ProcessingLog mockLog;
    private ProcessingLogDTO mockLogDTO;

    @BeforeEach
    void setUp() {
        recordId = UUID.randomUUID();
        staffId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        mockRequest = new Request();
        mockRequest.setId(recordId);
        mockRequest.setStatus("New");

        mockLog = new ProcessingLog();
        mockLog.setId(UUID.randomUUID());
        mockLog.setRecordId(recordId);
        mockLog.setStaffInCharge(staffId);
        mockLog.setStaffInChargeName("Test Staff");
        mockLog.setContent("Initial log");
        mockLog.setRequestStatus("New");
        mockLog.setCreatedAt(now);

        mockLogDTO = new ProcessingLogDTO(
                mockLog.getId(), "Request", recordId, staffId,
                "New log content", "Processing", "Repy", "Test Staff", null
        );
    }

    @Test
    @DisplayName("Test getProcessingLogsById - When find logs")
    void getProcessingLogsById_whenLogsFound_shouldReturnDtoList() {
        // Arrange
        when(processingLogRepository.findByRecordIdOrderByCreatedAtDesc(recordId))
                .thenReturn(List.of(mockLog));

        // Act
        List<ProcessingLogDTO> result = processingLogService.getProcessingLogsById(recordId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(mockLog.getId(), result.get(0).getId());
        verify(processingLogRepository).findByRecordIdOrderByCreatedAtDesc(recordId);
    }

    @Test
    @DisplayName("Test getProcessingLogsById - When cannot find logs")
    void getProcessingLogsById_whenNoLogsFound_shouldReturnEmptyList() {
        // Arrange
        when(processingLogRepository.findByRecordIdOrderByCreatedAtDesc(recordId))
                .thenReturn(Collections.emptyList());

        // Act
        List<ProcessingLogDTO> result = processingLogService.getProcessingLogsById(recordId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test addProcessingLog - Sucess when Request")
    void addProcessingLog_whenRequestExists_shouldUpdateStatusAndSaveLog() {
        // Arrange
        when(requestRepository.findById(recordId)).thenReturn(Optional.of(mockRequest));

        when(processingLogRepository.save(any(ProcessingLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<ProcessingLog> logCaptor = ArgumentCaptor.forClass(ProcessingLog.class);

        // Act
        ProcessingLogDTO result = processingLogService.addProcessingLog(recordId, mockLogDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Processing", result.getRequestStatus());
        assertEquals("New log content", result.getContent());

        verify(requestRepository).save(requestCaptor.capture());
        Request capturedRequest = requestCaptor.getValue();
        assertEquals("Processing", capturedRequest.getStatus());

        // Kiểm tra xem processingLogRepository.save() có được gọi không
        verify(processingLogRepository).save(logCaptor.capture());
        ProcessingLog capturedLog = logCaptor.getValue();
        assertEquals(recordId, capturedLog.getRecordId());
        assertEquals("Test Staff", capturedLog.getStaffInChargeName());
    }

    @Test
    @DisplayName("Test addProcessingLog - Failed when Request not found")
    void addProcessingLog_whenRequestNotFound_shouldThrowException() {
        // Arrange
        when(requestRepository.findById(recordId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processingLogService.addProcessingLog(recordId, mockLogDTO);
        });

        assertTrue(exception.getMessage().contains("Request not found with id: " + recordId));

        verify(requestRepository, never()).save(any(Request.class));
        verify(processingLogRepository, never()).save(any(ProcessingLog.class));
    }
}