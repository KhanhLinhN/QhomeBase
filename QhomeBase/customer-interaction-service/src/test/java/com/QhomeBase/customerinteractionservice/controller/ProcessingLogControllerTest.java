package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.service.processingLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingLogControllerTest {

    @Mock
    private processingLogService processingLogService;

    @InjectMocks
    private requestProcessingLogController requestProcessingLogController;

    private UUID sampleRequestId;
    private UUID sampleStaffId;
    private ProcessingLogDTO sampleLogDTO;

    @BeforeEach
    void setUp() {
        sampleRequestId = UUID.randomUUID();
        sampleStaffId = UUID.randomUUID();
        sampleLogDTO = new ProcessingLogDTO(
                UUID.randomUUID(),
                "Request",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sample content",
                "IN_PROGRESS",
                "UPDATE",
                "Supporter",
                "supporter@example.com",
                LocalDateTime.now().toString()
        );
    }

    @Test
    @DisplayName("GET /{id} - Find logs success")
    void getProcessingLog_whenLogsExist_shouldReturnDtoList() {
        // Arrange
        when(processingLogService.getProcessingLogsById(sampleRequestId))
                .thenReturn(List.of(sampleLogDTO));

        // Act
        List<ProcessingLogDTO> result = requestProcessingLogController.getProcessingLog(sampleRequestId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Updated status to Progressing.", result.get(0).getContent());
        verify(processingLogService).getProcessingLogsById(sampleRequestId);
    }

    @Test
    @DisplayName("GET /{id} - Return empty list logs")
    void getProcessingLog_whenNoLogsExist_shouldReturnEmptyList() {
        // Arrange
        when(processingLogService.getProcessingLogsById(any(UUID.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<ProcessingLogDTO> result = requestProcessingLogController.getProcessingLog(UUID.randomUUID());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GET /staff/{staffId} - Find log sucess for staff")
    void getProcessingLogsByStaffId_whenLogsExist_shouldReturnDtoList() {
        // Arrange
        when(processingLogService.getProcessingLogsByStaffId(sampleStaffId))
                .thenReturn(List.of(sampleLogDTO));

        // Act
        List<ProcessingLogDTO> result = requestProcessingLogController.getProcessingLogsByStaffId(sampleStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleStaffId, result.get(0).getStaffInCharge());
        verify(processingLogService).getProcessingLogsByStaffId(sampleStaffId);
    }


    @Test
    @DisplayName("POST /{requestId}/logs - Success")
    void addNewProcessLog_whenValid_shouldReturnCreatedLog() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(processingLogService.addProcessingLog(any(UUID.class), any(ProcessingLogDTO.class), any(Authentication.class)))
                .thenReturn(sampleLogDTO);

        // Act
        ResponseEntity<ProcessingLogDTO> response = requestProcessingLogController.addNewProcessLog(
                sampleRequestId,
                sampleLogDTO,
                auth
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleLogDTO, response.getBody());
        verify(processingLogService).addProcessingLog(sampleRequestId, sampleLogDTO, auth);
    }

    @Test
    @DisplayName("POST /{requestId}/logs - When service throws error")
    void addNewProcessLog_whenServiceFails_shouldPropagateException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(processingLogService.addProcessingLog(any(UUID.class), any(ProcessingLogDTO.class), any(Authentication.class)))
                .thenThrow(new RuntimeException("Boom"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                requestProcessingLogController.addNewProcessLog(sampleRequestId, sampleLogDTO, auth)
        );
    }
}