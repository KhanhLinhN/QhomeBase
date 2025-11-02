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
public class ServiceDto {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String code;
    private String name;
    private String description;
    private String location;
    private String mapUrl;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerSession;
    private String pricingType;
    private Integer maxCapacity;
    private Integer minDurationHours;
    private Integer maxDurationHours;
    private Integer advanceBookingDays;
    private String rules;
    private Boolean isActive;
}

