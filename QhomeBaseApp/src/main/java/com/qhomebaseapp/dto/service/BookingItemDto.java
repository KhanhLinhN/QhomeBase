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
public class BookingItemDto {
    private Long id;
    private String itemType; // OPTION, COMBO, TICKET
    private Long itemId;
    private String itemCode; // Mã của item (để dễ tra cứu)
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String metadata; // JSON metadata
}

