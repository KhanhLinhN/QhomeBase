package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.constants.ServiceCode;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreateInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ImportedReadingDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.dto.MeterReadingImportResponse;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
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
    private final BaseServiceClient baseServiceClient;

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

        Map<UUID, ReadingCycleDto> cycleCache = new HashMap<>();
        
        int created = 0;
        int skipped = 0;
        List<UUID> invoiceIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (Map.Entry<String, List<ImportedReadingDto>> entry : grouped.entrySet()) {
            List<ImportedReadingDto> group = entry.getValue();
            ImportedReadingDto head = group.get(0);

            UUID unitId = head.getUnitId();
            UUID residentId = head.getResidentId();
            UUID readingCycleId = head.getCycleId(); 

            try {
                ReadingCycleDto readingCycle = cycleCache.computeIfAbsent(readingCycleId, cycleId -> {
                    try {
                        ReadingCycleDto cycle = baseServiceClient.getReadingCycleById(cycleId);
                        if (cycle == null) {
                            log.error("Reading cycle not found: {}", cycleId);
                            throw new IllegalStateException("Reading cycle not found: " + cycleId);
                        }
                        return cycle;
                    } catch (Exception e) {
                        log.error("Error fetching reading cycle {}: {}", cycleId, e.getMessage());
                        throw new RuntimeException("Failed to fetch reading cycle: " + e.getMessage(), e);
                    }
                });

                if (!"COMPLETED".equalsIgnoreCase(readingCycle.status())) {
                    String errorMsg = String.format("Unit %s, Cycle %s: Cycle status is %s, must be COMPLETED", 
                            unitId, readingCycleId, readingCycle.status());
                    log.warn("Cannot create invoice for unit={}, cycle={}. Cycle status is {}, must be COMPLETED", 
                            unitId, readingCycleId, readingCycle.status());
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }
            } catch (Exception e) {
                String errorMsg = String.format("Unit %s, Cycle %s: %s", unitId, readingCycleId, e.getMessage());
                log.error("Error processing unit={}, cycle={}: {}", unitId, readingCycleId, e.getMessage());
                errors.add(errorMsg);
                skipped++;
                continue;
            }

            try {
                BigDecimal totalUsage = group.stream()
                        .map(ImportedReadingDto::getUsageKwh)
                        .filter(Objects::nonNull)
                        .filter(usage -> usage.compareTo(BigDecimal.ZERO) > 0)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalUsage.compareTo(BigDecimal.ZERO) == 0) {
                    String errorMsg = String.format("Unit %s, Cycle %s: Total usage is 0", unitId, readingCycleId);
                    log.warn("Total usage is 0 for unit={}, cycle={}. Readings: {}", 
                            unitId, readingCycleId, group.stream()
                                    .map(r -> String.format("usageKwh=%s", r.getUsageKwh()))
                                    .collect(Collectors.joining(", ")));
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }

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
                
                if (invoiceLines.isEmpty()) {
                    String errorMsg = String.format("Unit %s, Cycle %s: No invoice lines calculated (serviceCode=%s, totalUsage=%s)", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    log.warn("No invoice lines calculated for unit={}, cycle={}, serviceCode={}, totalUsage={}. Skipping invoice creation.", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }
                
                boolean hasValidPrice = invoiceLines.stream()
                        .anyMatch(line -> line.getUnitPrice() != null && line.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);
                
                if (!hasValidPrice) {
                    String errorMsg = String.format("Unit %s, Cycle %s: No valid pricing found (serviceCode=%s, totalUsage=%s)", 
                            unitId, readingCycleId, serviceCode, totalUsage);
                    log.warn("No valid pricing found for unit={}, cycle={}, serviceCode={}, totalUsage={}. Invoice lines: {}. Skipping invoice creation.", 
                            unitId, readingCycleId, serviceCode, totalUsage, 
                            invoiceLines.stream()
                                    .map(line -> String.format("quantity=%s, unitPrice=%s", line.getQuantity(), line.getUnitPrice()))
                                    .collect(Collectors.joining(", ")));
                    errors.add(errorMsg);
                    skipped++;
                    continue;
                }

                LocalDate dueDate = calculateDueDate(serviceDate);
                
                CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                        .payerUnitId(unitId)
                        .payerResidentId(residentId)
                        .cycleId(billingCycleId)
                        .currency("VND")
                        .dueDate(dueDate)
                        .lines(invoiceLines)
                        .build();

                InvoiceDto invoice = invoiceService.createInvoice(req);
                invoiceIds.add(invoice.getId());
                created++;
                
                log.info("Created invoice {} for unit={}, readingCycle={}, billingCycle={} with usage={} kWh ({} tiers)",
                        invoice.getId(), unitId, readingCycleId, billingCycleId, totalUsage, invoiceLines.size());
            } catch (Exception e) {
                String errorMsg = String.format("Unit %s, Cycle %s: %s", unitId, readingCycleId, e.getMessage());
                log.error("Error creating invoice for unit={}, cycle={}: {}", unitId, readingCycleId, e.getMessage(), e);
                errors.add(errorMsg);
                skipped++;
            }
        }

        String message;
        if (created > 0 && skipped == 0) {
            message = String.format("Successfully imported %d readings and created %d invoices", 
                    readings.size(), created);
        } else if (created > 0 && skipped > 0) {
            message = String.format("Imported %d readings: created %d invoices, skipped %d units", 
                    readings.size(), created, skipped);
        } else if (skipped > 0) {
            message = String.format("Failed to create invoices for %d units. See errors for details.", skipped);
        } else {
            message = "No invoices created";
        }

        return MeterReadingImportResponse.builder()
                .totalReadings(readings.size())
                .invoicesCreated(created)
                .invoicesSkipped(skipped)
                .invoiceIds(invoiceIds)
                .errors(errors.isEmpty() ? null : errors)
                .message(message)
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
    
    private LocalDate calculateDueDate(LocalDate serviceDate) {
        LocalDate endOfMonth = serviceDate.withDayOfMonth(serviceDate.lengthOfMonth());
        return endOfMonth.plusDays(7);
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
        if (readingCycleId != null) {
            List<BillingCycle> linkedCycles = billingCycleRepository.findByExternalCycleId(readingCycleId);
            if (!linkedCycles.isEmpty()) {
                BillingCycle linked = linkedCycles.get(0);
                log.debug("Reusing billing cycle {} already linked to reading cycle {}", linked.getId(), readingCycleId);
                return linked.getId();
            }
        }

        LocalDate periodFrom = serviceDate.withDayOfMonth(1);
        LocalDate periodTo = periodFrom.plusMonths(1).minusDays(1);
        
        List<BillingCycle> existing = billingCycleRepository.findListByTime(periodFrom, periodTo);
        for (BillingCycle cycle : existing) {
            if (readingCycleId != null) {
                if (readingCycleId.equals(cycle.getExternalCycleId())) {
                    log.debug("Found billing cycle {} already linked to reading cycle {}", cycle.getId(), readingCycleId);
                    return cycle.getId();
                }
                if (cycle.getExternalCycleId() == null) {
                    cycle.setExternalCycleId(readingCycleId);
                    BillingCycle saved = billingCycleRepository.save(cycle);
                    log.debug("Linked existing billing cycle {} to reading cycle {}", saved.getId(), readingCycleId);
                    return saved.getId();
                }
            } else {
                log.debug("Reusing existing billing cycle {} for period {} - {} with no external link",
                        cycle.getId(), periodFrom, periodTo);
                return cycle.getId();
            }
        }
        
        BillingCycle newCycle = BillingCycle.builder()
                .name(String.format("Cycle %s (%s)", 
                        periodFrom.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        readingCycleId != null ? readingCycleId.toString().substring(0, 8) : "AUTO"))
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .status("ACTIVE")
                .externalCycleId(readingCycleId)
                .build();
        
        BillingCycle saved = billingCycleRepository.save(newCycle);
        log.warn("Created new BillingCycle {} for readingCycleId {} (period {} to {})",
                saved.getId(), readingCycleId, periodFrom, periodTo);
        
        return saved.getId();
    }
}


