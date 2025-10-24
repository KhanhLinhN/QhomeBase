package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.ProRataInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ProRataInvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingBillingService {
    
    public ProRataInvoiceResponse createProRataInvoice(ProRataInvoiceRequest request) {
        try {
            validateRequest(request);
            return calculateProRataFee(request);
        } catch (Exception e) {
            return ProRataInvoiceResponse.builder()
                    .vehicleId(request.getVehicleId())
                    .plateNo(request.getPlateNo())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }
    
    private ProRataInvoiceResponse calculateProRataFee(ProRataInvoiceRequest request) {
        OffsetDateTime activatedAt = request.getActivatedAt();
        LocalDate activatedDate = activatedAt.toLocalDate();
        LocalDate endOfMonth = activatedDate.withDayOfMonth(activatedDate.lengthOfMonth());
        
        long chargedDays = ChronoUnit.DAYS.between(activatedDate, endOfMonth) + 1;
        int daysInMonth = activatedDate.lengthOfMonth();
        
        BigDecimal monthlyPrice = getMonthlyPrice(request.getVehicleKind());
        BigDecimal dailyRate = monthlyPrice.divide(
            BigDecimal.valueOf(daysInMonth), 
            2, 
            RoundingMode.HALF_UP
        );
        BigDecimal proRataAmount = dailyRate.multiply(BigDecimal.valueOf(chargedDays))
                                           .setScale(0, RoundingMode.HALF_UP);
        
        return ProRataInvoiceResponse.builder()
                .invoiceId(UUID.randomUUID())
                .vehicleId(request.getVehicleId())
                .unitId(request.getUnitId())
                .plateNo(request.getPlateNo())
                .amount(proRataAmount)
                .daysInMonth(daysInMonth)
                .chargedDays((int) chargedDays)
                .periodFrom(activatedAt)
                .periodTo(endOfMonth.atTime(23, 59, 59).atOffset(activatedAt.getOffset()))
                .status("SUCCESS")
                .message("Pro-rata invoice created successfully")
                .build();
    }
    
    private BigDecimal getMonthlyPrice(String vehicleKind) {
        return switch (vehicleKind != null ? vehicleKind : "CAR") {
            case "CAR" -> BigDecimal.valueOf(500000);
            case "MOTORBIKE" -> BigDecimal.valueOf(200000);
            case "BICYCLE" -> BigDecimal.valueOf(50000);
            default -> BigDecimal.valueOf(500000);
        };
    }
    
    private void validateRequest(ProRataInvoiceRequest request) {
        if (request.getVehicleId() == null) {
            throw new IllegalArgumentException("Vehicle ID is required");
        }
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request.getUnitId() == null) {
            throw new IllegalArgumentException("Unit ID is required");
        }
        if (request.getActivatedAt() == null) {
            throw new IllegalArgumentException("Activated date is required");
        }
    }
}
