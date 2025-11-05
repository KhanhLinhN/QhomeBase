package com.qhomebaseapp.dto.elevatorcard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorCardRegistrationResponseDto {
    private Long id;
    private String fullName;
    private String apartmentNumber;
    private String buildingName;
    private String requestType;
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

