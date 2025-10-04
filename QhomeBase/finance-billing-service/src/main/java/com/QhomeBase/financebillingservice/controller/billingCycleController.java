package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.billingCycleDto;
import com.QhomeBase.financebillingservice.service.billingCycleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/billing-cycles")
public class billingCycleController {
    private final billingCycleService billingCycleService;
    public billingCycleController(billingCycleService billingCycleService) {
        this.billingCycleService = billingCycleService;
    }
    @GetMapping("/loadPeriod")
    public List<billingCycleDto> findByTenantIdAndYear(@RequestParam UUID tenantId, @RequestParam Integer year) {
        int y =(year != null) ? year: LocalDate.now().getYear();
        return billingCycleService.loadPeriod(tenantId, y);
    }
    public List<billingCycleDto> getBillingCycle(@RequestParam UUID tenantId, @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return billingCycleService.getListByTime(tenantId, startDate, endDate);
    }

}
