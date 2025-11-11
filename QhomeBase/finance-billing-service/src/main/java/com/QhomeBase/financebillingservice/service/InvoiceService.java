package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.*;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    
    public List<InvoiceDto> getInvoicesByResident(UUID residentId) {
        List<Invoice> invoices = invoiceRepository.findByPayerResidentId(residentId);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByResidentAndStatus(UUID residentId, InvoiceStatus status) {
        List<Invoice> invoices = invoiceRepository.findByPayerResidentIdAndStatus(residentId, status);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public InvoiceDto getInvoiceById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return toDto(invoice);
    }
    
    public List<InvoiceDto> getInvoicesByUnit(UUID unitId) {
        List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitId);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByServiceCode(String serviceCode) {
        List<InvoiceLine> lines = invoiceLineRepository.findByServiceCode(serviceCode);
        List<UUID> invoiceIds = lines.stream()
                .map(InvoiceLine::getInvoiceId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);
        return invoices.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public List<InvoiceDto> getInvoicesByResidentAndServiceCode(UUID residentId, String serviceCode) {
        List<Invoice> allInvoices = invoiceRepository.findByPayerResidentId(residentId);
        return allInvoices.stream()
                .filter(invoice -> {
                    List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdAndServiceCode(
                            invoice.getId(), serviceCode);
                    return !lines.isEmpty();
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public InvoiceDto createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice for unit: {}", request.getPayerUnitId());

        String invoiceCode = generateInvoiceCode();

        Invoice invoice = Invoice.builder()
                .code(invoiceCode)
                .issuedAt(OffsetDateTime.now())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.DRAFT)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .billToName(request.getBillToName())
                .billToAddress(request.getBillToAddress())
                .billToContact(request.getBillToContact())
                .payerUnitId(request.getPayerUnitId())
                .payerResidentId(request.getPayerResidentId())
                .cycleId(request.getCycleId())
                .build();
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice created with ID: {}, code: {}", savedInvoice.getId(), savedInvoice.getCode());
        
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            for (CreateInvoiceLineRequest lineRequest : request.getLines()) {
                BigDecimal taxAmount = calculateTaxAmount(
                        lineRequest.getQuantity(),
                        lineRequest.getUnitPrice(),
                        lineRequest.getTaxRate()
                );
                
                InvoiceLine line = InvoiceLine.builder()
                        .invoiceId(savedInvoice.getId())
                        .serviceDate(lineRequest.getServiceDate())
                        .description(lineRequest.getDescription())
                        .quantity(lineRequest.getQuantity())
                        .unit(lineRequest.getUnit())
                        .unitPrice(lineRequest.getUnitPrice())
                        .taxRate(lineRequest.getTaxRate() != null ? lineRequest.getTaxRate() : BigDecimal.ZERO)
                        .taxAmount(taxAmount)
                        .serviceCode(lineRequest.getServiceCode())
                        .externalRefType(lineRequest.getExternalRefType())
                        .externalRefId(lineRequest.getExternalRefId())
                        .build();
                
                invoiceLineRepository.save(line);
            }
            log.info("Created {} invoice lines for invoice: {}", request.getLines().size(), savedInvoice.getId());
        }
        
        return toDto(savedInvoice);
    }
    
    @Transactional
    public InvoiceDto updateInvoiceStatus(UUID invoiceId, UpdateInvoiceStatusRequest request) {
        log.info("Updating invoice status: {} to {}", invoiceId, request.getStatus());
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(request.getStatus());
        
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        log.info("Invoice {} status updated from {} to {}", invoiceId, oldStatus, request.getStatus());
        
        return toDto(updatedInvoice);
    }
    
    @Transactional
    public void voidInvoice(UUID invoiceId, String reason) {
        log.info("Voiding invoice: {} with reason: {}", invoiceId, reason);
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot void a paid invoice. Create a refund instead.");
        }
        
        invoice.setStatus(InvoiceStatus.VOID);
        invoiceRepository.save(invoice);
        
        log.info("Invoice {} voided successfully", invoiceId);
    }
    
    private String generateInvoiceCode() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("INV-%s", timestamp);
    }
    
    private BigDecimal calculateTaxAmount(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = quantity.multiply(unitPrice);
        return subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
    
    private InvoiceDto toDto(Invoice invoice) {
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        
        BigDecimal totalAmount = lines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return InvoiceDto.builder()
                .id(invoice.getId())
                .code(invoice.getCode())
                .issuedAt(invoice.getIssuedAt())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .currency(invoice.getCurrency())
                .billToName(invoice.getBillToName())
                .billToAddress(invoice.getBillToAddress())
                .billToContact(invoice.getBillToContact())
                .payerUnitId(invoice.getPayerUnitId())
                .payerResidentId(invoice.getPayerResidentId())
                .cycleId(invoice.getCycleId())
                .totalAmount(totalAmount)
                .lines(lines.stream().map(this::lineToDto).collect(Collectors.toList()))
                .build();
    }
    
    private InvoiceLineDto lineToDto(InvoiceLine line) {
        return InvoiceLineDto.builder()
                .id(line.getId())
                .invoiceId(line.getInvoiceId())
                .serviceDate(line.getServiceDate())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unit(line.getUnit())
                .unitPrice(line.getUnitPrice())
                .taxRate(line.getTaxRate())
                .taxAmount(line.getTaxAmount())
                .lineTotal(line.getLineTotal())
                .serviceCode(line.getServiceCode())
                .externalRefType(line.getExternalRefType())
                .externalRefId(line.getExternalRefId())
                .build();
    }
}

