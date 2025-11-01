package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.constants.ServiceCode;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ImportedReadingDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.dto.MeterReadingImportResponse;
import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.PricingTier;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.PricingTierRepository;
import com.QhomeBase.financebillingservice.repository.ServicePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingImportService {

    private final ServicePricingRepository pricingRepository;
    private final PricingTierRepository pricingTierRepository;
    private final InvoiceService invoiceService;
    private final BillingCycleRepository billingCycleRepository;
    private final InvoiceRepository invoiceRepository;

    public int importReadings(List<ImportedReadingDto> readings) {
        MeterReadingImportResponse response = importReadingsWithResponse(readings);
        return response.getInvoicesCreated();
    }

    @Transactional
    public MeterReadingImportResponse importReadingsWithResponse(List<ImportedReadingDto> readings) {
        if (readings == null || readings.isEmpty()) {
            return MeterReadingImportResponse.builder()
                    .totalReadings(0)
                    .invoicesCreated(0)
                    .invoiceIds(Collections.emptyList())
                    .message("No readings to import")
                    .build();
        }

        Map<String, List<ImportedReadingDto>> grouped = readings.stream()
                .collect(Collectors.groupingBy(r -> key(r.getUnitId(), r.getCycleId())));

        int created = 0;
        List<UUID> invoiceIds = new ArrayList<>();
        
        for (Map.Entry<String, List<ImportedReadingDto>> entry : grouped.entrySet()) {
            List<ImportedReadingDto> group = entry.getValue();
            ImportedReadingDto head = group.get(0);

            UUID unitId = head.getUnitId();
            UUID residentId = head.getResidentId();
            UUID readingCycleId = head.getCycleId(); 

            BigDecimal totalUsage = group.stream()
                    .map(ImportedReadingDto::getUsageKwh)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDate serviceDate = group.stream()
                    .map(ImportedReadingDto::getReadingDate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(LocalDate.now());

            String rawServiceCode = head.getServiceCode();
            String serviceCode = normalizeServiceCode(rawServiceCode);
            
            if (!isServiceCodeValid(serviceCode)) {
                log.warn("Invalid or unknown service code: {} (normalized from: {}), defaulting to ELECTRIC", 
                        serviceCode, rawServiceCode);
                serviceCode = ServiceCode.ELECTRIC;
            }
            
            String description = Optional.ofNullable(head.getDescription())
                    .orElse(getDefaultDescription(serviceCode));

            UUID billingCycleId = findOrCreateBillingCycle(readingCycleId, serviceDate);

            List<Invoice> existingInvoices = invoiceRepository.findByPayerUnitIdAndCycleId(unitId, billingCycleId);
            if (!existingInvoices.isEmpty()) {
                Invoice existingInvoice = existingInvoices.get(0);
                log.warn("Invoice already exists for unit={}, cycle={}. Invoice ID: {}. Skipping creation.", 
                        unitId, billingCycleId, existingInvoice.getId());
                invoiceIds.add(existingInvoice.getId());
                continue;
            }

            List<CreateInvoiceLineRequest> invoiceLines = calculateInvoiceLines(
                    serviceCode, totalUsage, serviceDate, description);

            CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                    .payerUnitId(unitId)
                    .payerResidentId(residentId)
                    .cycleId(billingCycleId)
                    .currency("VND")
                    .lines(invoiceLines)
                    .build();

            InvoiceDto invoice = invoiceService.createInvoice(req);
            invoiceIds.add(invoice.getId());
            created++;
            
            log.info("Created invoice {} for unit={}, readingCycle={}, billingCycle={} with usage={} kWh ({} tiers)",
                    invoice.getId(), unitId, readingCycleId, billingCycleId, totalUsage, invoiceLines.size());
        }

        return MeterReadingImportResponse.builder()
                .totalReadings(readings.size())
                .invoicesCreated(created)
                .invoiceIds(invoiceIds)
                .message(String.format("Successfully imported %d readings and created %d invoices", 
                        readings.size(), created))
                .build();
    }

    private String key(UUID unitId, UUID cycleId) {
        return unitId + "|" + cycleId;
    }

    private List<CreateInvoiceLineRequest> calculateInvoiceLines(
            String serviceCode, BigDecimal totalUsage, LocalDate serviceDate, String baseDescription) {
        
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, serviceDate);
        
        if (tiers.isEmpty()) {
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        BigDecimal previousMax = BigDecimal.ZERO;
        
        for (PricingTier tier : tiers) {
            if (previousMax.compareTo(totalUsage) >= 0) {
                break;
            }
            
            BigDecimal tierEffectiveMax;
            if (tier.getMaxQuantity() == null) {
                tierEffectiveMax = totalUsage;
            } else {
                tierEffectiveMax = totalUsage.min(tier.getMaxQuantity());
            }
            
            BigDecimal applicableQuantity = tierEffectiveMax.subtract(previousMax).max(BigDecimal.ZERO);
            
            if (applicableQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tierAmount = applicableQuantity.multiply(tier.getUnitPrice());
                
                String maxQtyStr = tier.getMaxQuantity() != null ? tier.getMaxQuantity().toString() : "∞";
                String tierDescription = String.format("%s (Bậc %d: %s-%s kWh)",
                        baseDescription,
                        tier.getTierOrder(),
                        tier.getMinQuantity(),
                        maxQtyStr);
                
                CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(tierDescription)
                        .quantity(applicableQuantity)
                        .unit("kWh")
                        .unitPrice(tier.getUnitPrice())
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode(serviceCode)
                        .externalRefType("METER_READING_GROUP")
                        .externalRefId(null)
                        .build();
                
                lines.add(line);
                previousMax = tierEffectiveMax;
                
                log.debug("Tier {}: {} kWh × {} VND/kWh = {} VND", 
                        tier.getTierOrder(), applicableQuantity, tier.getUnitPrice(), tierAmount);
            }
        }
        
        if (lines.isEmpty()) {
            log.warn("No tiers matched for usage {} kWh, using simple pricing", totalUsage);
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        return lines;
    }

    private String normalizeServiceCode(String rawServiceCode) {
        if (rawServiceCode == null || rawServiceCode.trim().isEmpty()) {
            return ServiceCode.ELECTRIC;
        }
        return ServiceCode.normalize(rawServiceCode);
    }
    
    private boolean isServiceCodeValid(String serviceCode) {
        return ServiceCode.isValid(serviceCode);
    }
    
    private String getDefaultDescription(String serviceCode) {
        if (ServiceCode.ELECTRIC.equals(serviceCode)) {
            return "Tiền điện";
        }
        if (ServiceCode.WATER.equals(serviceCode)) {
            return "Tiền nước";
        }
        return "Tiền dịch vụ";
    }
    
    private BigDecimal resolveUnitPrice(String serviceCode, LocalDate date) {
        return pricingRepository.findActivePriceGlobal(serviceCode, date)
                .map(sp -> sp.getBasePrice())
                .orElse(BigDecimal.ZERO);
    }


    private UUID findOrCreateBillingCycle(UUID readingCycleId, LocalDate serviceDate) {
        LocalDate periodFrom = serviceDate.withDayOfMonth(1);
        LocalDate periodTo = periodFrom.plusMonths(1).minusDays(1);
        
        List<BillingCycle> existing = billingCycleRepository.findListByTime(periodFrom, periodTo);
        if (!existing.isEmpty()) {
            UUID billingCycleId = existing.get(0).getId();
            log.debug("Found existing BillingCycle {} for period {} to {}", 
                    billingCycleId, periodFrom, periodTo);
            return billingCycleId;
        }
        
        BillingCycle newCycle = BillingCycle.builder()
                .name(String.format("Cycle %s", periodFrom.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))))
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .status("ACTIVE")
                .build();
        
        BillingCycle saved = billingCycleRepository.save(newCycle);
        log.warn("Created new BillingCycle {} for readingCycleId {} (period {} to {})",
                saved.getId(), readingCycleId, periodFrom, periodTo);
        
        return saved.getId();
    }
}


