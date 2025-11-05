package com.qhomebaseapp.dto.residentcard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentCardRegistrationResponseDto {
    private Long id;
    private String residentName;
    private String apartmentNumber;
    private String buildingName;
    private String citizenId;
    private String phoneNumber;
    private String note;
    private String status; // PENDING
    private String paymentStatus; // PAID, UNPAID
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime paymentDate;
    private String paymentGateway;
    private String vnpayTransactionRef;
    private Long userId;
    private String userEmail;
}

