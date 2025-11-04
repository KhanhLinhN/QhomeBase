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
public class ServiceOptionDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String unit;
    private Boolean isRequired;
    private Boolean isActive;
    private Integer sortOrder;
}

