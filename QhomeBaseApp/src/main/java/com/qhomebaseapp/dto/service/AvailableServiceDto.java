package com.qhomebaseapp.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableServiceDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String location;
    private String mapUrl;
    private BigDecimal pricePerHour;
    private BigDecimal estimatedTotalAmount; // Tính dựa trên số giờ
    private Integer maxCapacity;
    private Integer minDurationHours;
    private Integer maxDurationHours;
    private String rules;
    private Boolean isAvailable; // Slot có available không
    private String availabilityStatus; // "AVAILABLE", "FULL", "PARTIAL"
    private Integer currentBookings; // Số booking hiện có trong khoảng thời gian này
}

