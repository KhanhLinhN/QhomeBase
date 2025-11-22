package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.BuildingInvoiceSummaryDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.service.BillingCycleInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing-cycles/{cycleId}")
@RequiredArgsConstructor
@Slf4j
public class BillingCycleInvoiceController {

    private final BillingCycleInvoiceService billingCycleInvoiceService;

    @GetMapping("/buildings")
    public List<BuildingInvoiceSummaryDto> getBuildingSummary(
            @PathVariable UUID cycleId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month
    ) {
        return billingCycleInvoiceService.summarizeByCycle(cycleId, serviceCode, month);
    }

    @GetMapping("/buildings/{buildingId}/invoices")
    public List<InvoiceDto> getInvoicesByBuilding(
            @PathVariable UUID cycleId,
            @PathVariable UUID buildingId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month) {
        return billingCycleInvoiceService.getInvoicesByBuilding(cycleId, buildingId, serviceCode, month);
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> getInvoicesByCycle(
            @PathVariable UUID cycleId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month) {
        return billingCycleInvoiceService.getInvoicesByCycle(cycleId, serviceCode, month);
    }
}

