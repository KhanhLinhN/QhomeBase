package com.qhomebaseapp.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBookingResponseDto {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private String serviceLocation;
    private Long userId;
    private String userName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal durationHours;
    private Integer numberOfPeople;
    private String purpose;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private OffsetDateTime paymentDate;
    private String paymentGateway;
    private String vnpayTransactionRef;
    private String status;
    private Boolean termsAccepted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

