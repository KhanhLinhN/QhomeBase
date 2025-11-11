package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.CreateInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.dto.UpdateInvoiceStatusRequest;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByResident(@PathVariable UUID residentId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResident(residentId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/resident/{residentId}/unpaid")
    public ResponseEntity<List<InvoiceDto>> getUnpaidInvoicesByResident(@PathVariable UUID residentId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResidentAndStatus(residentId, InvoiceStatus.PUBLISHED);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto> getInvoiceById(@PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }
    
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByUnit(@PathVariable UUID unitId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByUnit(unitId);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/service/{serviceCode}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByServiceCode(@PathVariable String serviceCode) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByServiceCode(serviceCode);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/resident/{residentId}/service/{serviceCode}")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByResidentAndServiceCode(
            @PathVariable UUID residentId,
            @PathVariable String serviceCode) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByResidentAndServiceCode(residentId, serviceCode);
        return ResponseEntity.ok(invoices);
    }
    
    @PostMapping
    public ResponseEntity<InvoiceDto> createInvoice(@RequestBody CreateInvoiceRequest request) {
        InvoiceDto invoice = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(
            @PathVariable UUID id,
            @RequestBody UpdateInvoiceStatusRequest request) {
        InvoiceDto invoice = invoiceService.updateInvoiceStatus(id, request);
        return ResponseEntity.ok(invoice);
    }
    
    @DeleteMapping("/{id}/void")
    public ResponseEntity<Void> voidInvoice(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        invoiceService.voidInvoice(id, reason);
        return ResponseEntity.noContent().build();
    }
}

