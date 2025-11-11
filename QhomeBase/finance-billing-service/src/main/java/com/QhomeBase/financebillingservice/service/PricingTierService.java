package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreatePricingTierRequest;
import com.QhomeBase.financebillingservice.dto.PricingTierDto;
import com.QhomeBase.financebillingservice.dto.UpdatePricingTierRequest;
import com.QhomeBase.financebillingservice.model.PricingTier;
import com.QhomeBase.financebillingservice.repository.PricingTierRepository;
import com.QhomeBase.financebillingservice.repository.ServicePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingTierService {
    private final PricingTierRepository pricingTierRepository;
    private final ServicePricingRepository pricingRepository;

    @Transactional
    public PricingTierDto createPricingTier(CreatePricingTierRequest req, UUID createdBy) {
        String serviceCode = req.getServiceCode();
        List<PricingTier> pricingTierList = pricingTierRepository.findActiveTiersByService(serviceCode);
        
        if (!pricingTierList.isEmpty()) {
            PricingTier lastPricingTier = pricingTierList.get(pricingTierList.size() - 1);
            BigDecimal lastMaxQuantity = lastPricingTier.getMaxQuantity();
            
            if (lastMaxQuantity != null && req.getMinQuantity().compareTo(lastMaxQuantity) < 0) {
                throw new IllegalArgumentException(
                    String.format("Next tier minQuantity (%s) must be >= previous tier maxQuantity (%s)",
                        req.getMinQuantity(), lastMaxQuantity));
            }
            
            if (req.getTierOrder() <= lastPricingTier.getTierOrder()) {
                throw new IllegalArgumentException(
                    String.format("Next tier order (%d) must be > previous tier order (%d)",
                        req.getTierOrder(), lastPricingTier.getTierOrder()));
            }
        }

        if (req.getMaxQuantity() != null && req.getMinQuantity().compareTo(req.getMaxQuantity()) >= 0) {
            throw new IllegalArgumentException(
                "minQuantity must be < maxQuantity");
        }

        OffsetDateTime now = OffsetDateTime.now();
        
        PricingTier newPricingTier = PricingTier.builder()
                .serviceCode(serviceCode)
                .tierOrder(req.getTierOrder())
                .minQuantity(req.getMinQuantity())
                .maxQuantity(req.getMaxQuantity())
                .unitPrice(req.getUnitPrice())
                .effectiveFrom(req.getEffectiveFrom())
                .effectiveUntil(req.getEffectiveUntil())
                .active(req.getActive() != null ? req.getActive() : true)
                .description(req.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(createdBy)
                .build();

        PricingTier saved = pricingTierRepository.save(newPricingTier);
        log.info("Created pricing tier: id={}, serviceCode={}, tierOrder={}", 
                saved.getId(), saved.getServiceCode(), saved.getTierOrder());
        return toDto(saved);
    }
    private PricingTierDto toDto(PricingTier pricingTier) {
        return PricingTierDto.builder()
                .id(pricingTier.getId())
                .serviceCode(pricingTier.getServiceCode())
                .tierOrder(pricingTier.getTierOrder())
                .minQuantity(pricingTier.getMinQuantity())
                .maxQuantity(pricingTier.getMaxQuantity())
                .unitPrice(pricingTier.getUnitPrice())
                .effectiveFrom(pricingTier.getEffectiveFrom())
                .effectiveUntil(pricingTier.getEffectiveUntil())
                .active(pricingTier.getActive())
                .description(pricingTier.getDescription())
                .createdAt(pricingTier.getCreatedAt())
                .updatedAt(pricingTier.getUpdatedAt())
                .build();
    }
    @Transactional(readOnly = true)
    public List<PricingTierDto> getAllPricingTiers(String serviceCode) {
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByService(serviceCode);
        return tiers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PricingTierDto getById(UUID id) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing tier not found: " + id));
        return toDto(tier);
    }

    @Transactional
    public PricingTierDto updatePricingTier(UUID id, UpdatePricingTierRequest req, UUID updatedBy) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing tier not found: " + id));

        if (req.getTierOrder() != null) {
            tier.setTierOrder(req.getTierOrder());
        }
        if (req.getMinQuantity() != null) {
            tier.setMinQuantity(req.getMinQuantity());
        }
        if (req.getMaxQuantity() != null) {
            tier.setMaxQuantity(req.getMaxQuantity());
        }
        if (req.getUnitPrice() != null) {
            tier.setUnitPrice(req.getUnitPrice());
        }
        if (req.getEffectiveFrom() != null) {
            tier.setEffectiveFrom(req.getEffectiveFrom());
        }
        if (req.getEffectiveUntil() != null) {
            tier.setEffectiveUntil(req.getEffectiveUntil());
        }
        if (req.getActive() != null) {
            tier.setActive(req.getActive());
        }
        if (req.getDescription() != null) {
            tier.setDescription(req.getDescription());
        }

        tier.setUpdatedAt(OffsetDateTime.now());
        tier.setUpdatedBy(updatedBy);

        PricingTier updated = pricingTierRepository.save(tier);
        log.info("Updated pricing tier: id={}, serviceCode={}", updated.getId(), updated.getServiceCode());
        return toDto(updated);
    }

    @Transactional
    public void deletePricingTier(UUID id) {
        if (!pricingTierRepository.existsById(id)) {
            throw new IllegalArgumentException("Pricing tier not found: " + id);
        }
        pricingTierRepository.deleteById(id);
        log.info("Deleted pricing tier: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<PricingTierDto> getActiveTiersByServiceAndDate(String serviceCode, LocalDate date) {
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, date);
        return tiers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Integer getLastOrder(String serviceCode) {
        List<PricingTier> pricingTierList = pricingTierRepository.findActiveTiersByService(serviceCode);
        if (pricingTierList.isEmpty()) {
            return 0;
        }
        return pricingTierList.get(pricingTierList.size() - 1).getTierOrder();
    }
    private BigDecimal resolveUnitPrice(String serviceCode, LocalDate date) {
        return pricingRepository.findActivePriceGlobal(serviceCode, date)
                .map(sp -> sp.getBasePrice())
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CreateInvoiceLineRequest> calculateInvoiceLines(
            String serviceCode, 
            BigDecimal totalUsage, 
            LocalDate serviceDate, 
            String baseDescription) {
        
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
}
