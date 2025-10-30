package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.billingCycleDto;
import com.QhomeBase.financebillingservice.model.billingCycle;
import com.QhomeBase.financebillingservice.repository.billingCycleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Service
public class billingCycleService {
    private final billingCycleRepository billingCycleRepository;
    public billingCycleService(billingCycleRepository billingCycleRepository) {
        this.billingCycleRepository = billingCycleRepository;
    }

    private billingCycleDto mapDto (billingCycle b) {
        return  new billingCycleDto(
                b.getId(),
                b.getTenantId(),
                b.getName(),
                b.getPeriodFrom(),
                b.getPeriodTo(),
                b.getStatus()
        );
    }
    public List<billingCycleDto> loadPeriod(UUID tenantId, Integer year) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        List<billingCycleDto> billingCycleDtos = new ArrayList<>();
        billingCycleDtos = billingCycleRepository.loadPeriod(tenantId, y).stream().map(this::mapDto).toList();
        return  billingCycleDtos;
    }
    public List<billingCycleDto> getListByTime(UUID tenantId,
                                                LocalDate periodFrom,
                                                LocalDate periodTo) {
        return billingCycleRepository.findListByTime(tenantId, periodFrom, periodTo)
                .stream()
                .map(this::mapDto)
                .toList();
    }
}
