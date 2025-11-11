package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingCycleRequest {
    private String name;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String status;
}




