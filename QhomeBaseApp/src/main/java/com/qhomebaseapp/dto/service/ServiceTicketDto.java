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
public class ServiceTicketDto {
    private Long id;
    private String code;
    private String name;
    private String ticketType; // DAY, NIGHT, HOURLY, DAILY, FAMILY
    private BigDecimal durationHours;
    private BigDecimal price;
    private Integer maxPeople; // Cho vé gia đình
    private String description;
    private Boolean isActive;
    private Integer sortOrder;
}

