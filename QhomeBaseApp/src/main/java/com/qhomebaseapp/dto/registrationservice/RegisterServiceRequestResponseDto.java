package com.qhomebaseapp.dto.registrationservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequestResponseDto {
    private Long id;
    private String serviceType;
    private String note;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long userId;
    private String userEmail;
}
