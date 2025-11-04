package com.qhomebaseapp.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarSlotDto {
    private Long id;
    private String code;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String note;
    private Boolean isActive;
    private Integer sortOrder;
}

