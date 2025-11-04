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
public class ServiceComboDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String servicesIncluded; // Dịch vụ bao gồm
    private Integer durationMinutes; // Thời lượng (phút)
    private BigDecimal price;
    private Boolean isActive;
    private Integer sortOrder;
}

