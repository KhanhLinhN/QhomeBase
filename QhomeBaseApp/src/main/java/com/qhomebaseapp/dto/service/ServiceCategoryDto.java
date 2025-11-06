package com.qhomebaseapp.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategoryDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
}

