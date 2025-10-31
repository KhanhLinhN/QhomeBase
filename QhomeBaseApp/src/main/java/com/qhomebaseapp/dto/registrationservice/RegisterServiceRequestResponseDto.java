package com.qhomebaseapp.dto.registrationservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequestResponseDto {
    private Long id;
    private String serviceType;
    private String note;
    private String status; // PENDING, DRAFT - trạng thái xử lý của admin
    private String paymentStatus; // PAID, UNPAID - trạng thái thanh toán
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime paymentDate;
    private String paymentGateway;
    private String vnpayTransactionRef;
    private Long userId;
    private String userEmail;
    private String vehicleType;
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleColor;
    private List<String> imageUrls;

}
