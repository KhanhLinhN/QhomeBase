package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.ProRataInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ProRataInvoiceResponse;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.model.ServicePricing;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.ServicePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingBillingService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final ServicePricingRepository servicePricingRepository;
    
    @Transactional
    public ProRataInvoiceResponse createProRataInvoice(ProRataInvoiceRequest request) {
        log.info("📋 Received pro-rata invoice request for vehicle: {} ({})", 
                request.getPlateNo(), request.getVehicleId());
        log.info("   Tenant: {}, Resident: {}, Unit: {}", 
                request.getTenantId(), request.getResidentId(), request.getUnitId());
        
        try {
            validateRequest(request);
            ProRataInvoiceResponse response = calculateAndSaveInvoice(request);
            log.info("✅ Pro-rata invoice created successfully: {} (amount: {} VND, {} days)", 
                    response.getInvoiceId(), response.getAmount(), response.getChargedDays());
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to create pro-rata invoice for vehicle: {} ({})", 
                    request.getPlateNo(), request.getVehicleId(), e);
            return ProRataInvoiceResponse.builder()
                    .vehicleId(request.getVehicleId())
                    .plateNo(request.getPlateNo())
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }
    
    private ProRataInvoiceResponse calculateAndSaveInvoice(ProRataInvoiceRequest request) {
        OffsetDateTime activatedAt = request.getActivatedAt();
        LocalDate activatedDate = activatedAt.toLocalDate();
        LocalDate endOfMonth = activatedDate.withDayOfMonth(activatedDate.lengthOfMonth());
        
        long chargedDays = ChronoUnit.DAYS.between(activatedDate, endOfMonth) + 1;
        int daysInMonth = activatedDate.lengthOfMonth();
        
        BigDecimal monthlyPrice = getMonthlyPrice(request.getVehicleKind(), request.getTenantId(), activatedDate);
        BigDecimal dailyRate = monthlyPrice.divide(
            BigDecimal.valueOf(daysInMonth), 
            2, 
            RoundingMode.HALF_UP
        );
        BigDecimal proRataAmount = dailyRate.multiply(BigDecimal.valueOf(chargedDays))
                                           .setScale(0, RoundingMode.HALF_UP);
        
        String invoiceCode = generateInvoiceCode(request.getTenantId());
        
        Invoice invoice = Invoice.builder()
                .tenantId(request.getTenantId())
                .code(invoiceCode)
                .issuedAt(OffsetDateTime.now())
                .dueDate(endOfMonth.plusDays(7))
                .status(InvoiceStatus.PUBLISHED)
                .currency("VND")
                .payerUnitId(request.getUnitId())
                .payerResidentId(request.getResidentId())
                .build();
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        String description = String.format("Phí gửi xe %s - %s (%d ngày)", 
                request.getVehicleKind(), 
                request.getPlateNo(),
                chargedDays);
        
        InvoiceLine line = InvoiceLine.builder()
                .tenantId(request.getTenantId())
                .invoiceId(savedInvoice.getId())
                .serviceDate(activatedDate)
                .description(description)
                .quantity(BigDecimal.valueOf(chargedDays))
                .unit("ngày")
                .unitPrice(dailyRate)
                .taxRate(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .serviceCode("PARKING_PRORATA")
                .externalRefType("VEHICLE")
                .externalRefId(request.getVehicleId())
                .build();
        
        invoiceLineRepository.save(line);
        
        return ProRataInvoiceResponse.builder()
                .invoiceId(savedInvoice.getId())
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
    
    private String generateInvoiceCode(UUID tenantId) {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tenantShort = tenantId.toString().substring(0, 8);
        return String.format("INV-%s-%s", tenantShort, timestamp);
    }
    
    private BigDecimal getMonthlyPrice(String vehicleKind, UUID tenantId, LocalDate effectiveDate) {
        String serviceCode = "PARKING_" + (vehicleKind != null ? vehicleKind : "CAR");
        
        return servicePricingRepository.findActivePrice(tenantId, serviceCode, effectiveDate)
                .map(ServicePricing::getBasePrice)
                .orElseGet(() -> {
                    log.warn("No active pricing found for service code: {} on date: {}. Using fallback price.", 
                            serviceCode, effectiveDate);
                    return getFallbackPrice(vehicleKind);
                });
    }
    
    private BigDecimal getFallbackPrice(String vehicleKind) {
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
