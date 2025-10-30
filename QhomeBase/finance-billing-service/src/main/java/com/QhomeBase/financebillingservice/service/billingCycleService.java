package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.BillingCycleDto;
import com.QhomeBase.financebillingservice.dto.CreateBillingCycleRequest;
import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCycleService {

    private final BillingCycleRepository billingCycleRepository;

    private BillingCycleDto mapDto(BillingCycle b) {
        return new BillingCycleDto(
                b.getId(),
                b.getTenantId(),
                b.getName(),
                b.getPeriodFrom(),
                b.getPeriodTo(),
                b.getStatus()
        );
    }

    public List<BillingCycleDto> loadPeriod(UUID tenantId, Integer year) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        return billingCycleRepository.loadPeriod(tenantId, y)
                .stream()
                .map(this::mapDto)
                .toList();
    }

    public List<BillingCycleDto> getListByTime(UUID tenantId, LocalDate periodFrom, LocalDate periodTo) {
        return billingCycleRepository.findListByTime(tenantId, periodFrom, periodTo)
                .stream()
                .map(this::mapDto)
                .toList();
    }

    @Transactional
    public BillingCycleDto createBillingCycle(CreateBillingCycleRequest request) {
        log.info("Creating billing cycle for tenant: {}, period: {} to {}",
                request.getTenantId(), request.getPeriodFrom(), request.getPeriodTo());

        if (request.getPeriodFrom().isAfter(request.getPeriodTo())) {
            throw new IllegalArgumentException("Period From must be before Period To");
        }

        BillingCycle billingCycle = BillingCycle.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .periodFrom(request.getPeriodFrom())
                .periodTo(request.getPeriodTo())
                .status(request.getStatus() != null ? request.getStatus() : "OPEN")
                .build();

        BillingCycle saved = billingCycleRepository.save(billingCycle);
        log.info("Billing cycle created with ID: {}", saved.getId());

        return mapDto(saved);
    }

    @Transactional
    public BillingCycleDto updateBillingCycleStatus(UUID cycleId, String status) {
        log.info("Updating billing cycle status: {} to {}", cycleId, status);

        BillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Billing cycle not found: " + cycleId));

        String oldStatus = cycle.getStatus();
        cycle.setStatus(status);

        BillingCycle updated = billingCycleRepository.save(cycle);
        log.info("Billing cycle {} status updated from {} to {}", cycleId, oldStatus, status);

        return mapDto(updated);
    }
}
