package com.qhomebaseapp.dto.bill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDto {
    private Long id;
    private String billType;
    private BigDecimal amount;
    private LocalDate billingMonth;
    private String status;
    private String description;
    private LocalDateTime paymentDate;
}
