package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.BuildingInvoiceSummaryDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.repository.BuildingInvoiceSummary;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCycleInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public List<BuildingInvoiceSummaryDto> summarizeByCycle(UUID cycleId, String serviceCode, String month) {
        List<BuildingInvoiceSummary> summaryRows = invoiceRepository.summarizeByCycleAndBuilding(cycleId, null, null);
        
        return summaryRows.stream()
                .map(row -> BuildingInvoiceSummaryDto.builder()
                        .buildingId(row.getBuildingId())
                        .buildingCode(row.getBuildingCode())
                        .buildingName(row.getBuildingName())
                        .status(row.getStatus())
                        .totalAmount(row.getTotalAmount())
                        .invoiceCount(row.getInvoiceCount())
                        .build())
                .collect(Collectors.toList());
    }

    public List<InvoiceDto> getInvoicesByCycle(UUID cycleId, String serviceCode, String month) {
        List<Invoice> invoices = invoiceRepository.findByCycleId(cycleId);
        return invoices.stream()
                .map(invoiceService::mapToDto)
                .collect(Collectors.toList());
    }

    public List<InvoiceDto> getInvoicesByBuilding(UUID cycleId, UUID buildingId, String serviceCode, String month) {
        return invoiceRepository.findByCycleIdAndBuildingId(cycleId, buildingId)
                .stream()
                .map(invoiceService::mapToDto)
                .collect(Collectors.toList());
    }
}

