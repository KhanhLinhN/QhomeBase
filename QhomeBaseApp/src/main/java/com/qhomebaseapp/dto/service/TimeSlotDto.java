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
public class TimeSlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private String reason; // Lý do nếu không available (optional)
    private Integer bookedPeople; // Số người đã book (nếu có)
    private Integer availableCapacity; // Số chỗ còn trống (nếu có)
}

