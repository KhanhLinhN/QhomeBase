package com.qhomebaseapp.dto.bill;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillStatisticsDto {
    private String month;
    private String billType;
    private BigDecimal totalAmount;
}
