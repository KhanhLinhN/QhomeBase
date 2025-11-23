package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardPricingRequest {
    
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String currency;

    private String description;

    private Boolean isActive;
}

