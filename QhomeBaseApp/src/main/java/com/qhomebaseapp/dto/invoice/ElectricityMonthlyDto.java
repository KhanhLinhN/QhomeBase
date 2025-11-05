package com.qhomebaseapp.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for electricity consumption by month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityMonthlyDto {
    private String month; // Format: "YYYY-MM"
    private String monthDisplay; // Format: "MM/yyyy"
    private BigDecimal amount; // Total electricity amount for this month
    private Integer year;
    private Integer monthNumber; // 1-12
}

