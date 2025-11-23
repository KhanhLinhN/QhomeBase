package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.config.VnpayProperties;
import com.QhomeBase.financebillingservice.dto.*;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.ResidentRepository;
import com.QhomeBase.financebillingservice.repository.ResidentRepository.ResidentContact;
import com.QhomeBase.financebillingservice.service.vnpay.VnpayService;
import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"NullAway", "null"})
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final ResidentRepository residentRepository;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final NotificationEmailService emailService;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;

    private final ConcurrentMap<Long, UUID> orderIdToInvoiceIdMap = new ConcurrentHashMap<>();

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "ELECTRICITY", "Điện",
            "WATER", "Nước",
            "INTERNET", "Internet",
            "ELEVATOR", "Vé thang máy",
            "PARKING", "Vé gửi xe",
            "OTHER", "Khác"
    );

    private static final List<String> CATEGORY_ORDER = List.of(
            "ELECTRICITY",
            "WATER",
            "INTERNET",
            "ELEVATOR",
            "PARKING",
            "OTHER"
    );
    
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
    public InvoiceDto recordVehicleRegistrationPayment(VehicleRegistrationPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildVehicleRegistrationDescription(request);

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(description)
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("VEHICLE_CARD")
                        .externalRefType("VEHICLE_REGISTRATION")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }
    
    @Transactional
    public InvoiceDto createInvoice(CreateInvoiceRequest request) {
        log.info("Creating invoice for unit: {}", request.getPayerUnitId());

        String invoiceCode = generateInvoiceCode();

        Invoice invoice = Invoice.builder()
                .code(invoiceCode)
                .issuedAt(OffsetDateTime.now())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.PUBLISHED)
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
        
        // Send notification to resident (only for electricity and water invoices)
        sendInvoiceNotification(savedInvoice);
        
        return toDto(savedInvoice);
    }
    
    private void sendInvoiceNotification(Invoice invoice) {
        if (invoice.getPayerResidentId() == null) {
            log.warn("⚠️ [InvoiceService] Cannot send notification: payerResidentId is null");
            return;
        }
        
        // Only send notification for electricity and water invoices
        // Skip notification for card payment invoices (VEHICLE_CARD, ELEVATOR_CARD, RESIDENT_CARD)
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
        boolean shouldSendNotification = false;
        
        for (InvoiceLine line : lines) {
            String serviceCode = line.getServiceCode();
            if (serviceCode != null) {
                String normalized = serviceCode.trim().toUpperCase();
                // Only send notification for electricity and water invoices
                if (normalized.contains("ELECTRICITY") || normalized.contains("WATER")) {
                    shouldSendNotification = true;
                    break;
                }
            }
        }
        
        if (!shouldSendNotification) {
            log.debug("ℹ️ [InvoiceService] Skipping notification for invoice {} - not an electricity/water invoice", invoice.getId());
            return;
        }
        
        try {
            // Get buildingId from unitId
            UUID buildingId = null;
            if (invoice.getPayerUnitId() != null) {
                try {
                    BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                    if (unitInfo != null && unitInfo.getBuildingId() != null) {
                        buildingId = unitInfo.getBuildingId();
                        log.info("✅ [InvoiceService] Resolved buildingId={} from unitId={}", buildingId, invoice.getPayerUnitId());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceService] Failed to get buildingId from unitId {}: {}", invoice.getPayerUnitId(), e.getMessage());
                }
            }
            
            // Calculate total amount
            BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Format amount
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            currencyFormat.setMaximumFractionDigits(0);
            String amountText = currencyFormat.format(totalAmount);
            
            // Build notification message
            String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
            String title = "Hóa đơn mới - " + invoiceCode;
            String message = String.format("Bạn có hóa đơn mới với số tiền %s. Hạn thanh toán: %s", 
                    amountText,
                    invoice.getDueDate() != null ? invoice.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
            
            // Prepare data payload
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("invoiceCode", invoiceCode);
            data.put("amount", totalAmount.toString());
            data.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
            
            // Send notification
            notificationClient.sendResidentNotification(
                    invoice.getPayerResidentId(),
                    buildingId,
                    "BILL",
                    title,
                    message,
                    invoice.getId(),
                    "INVOICE",
                    data
            );
            
            log.info("✅ [InvoiceService] Sent invoice notification to residentId={}, buildingId={}, invoiceId={}", 
                    invoice.getPayerResidentId(), buildingId, invoice.getId());
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Failed to send invoice notification for invoiceId={}: {}", 
                    invoice.getId(), e.getMessage(), e);
        }
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
    
    public List<InvoiceLineResponseDto> getMyInvoices(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        
        List<Invoice> invoices = invoiceRepository.findByPayerResidentId(residentId);
        invoices = invoices.stream()
                .filter(invoice -> unitFilter.equals(invoice.getPayerUnitId()))
                .filter(invoice -> cycleFilter == null || cycleFilter.equals(invoice.getCycleId()))
                .collect(Collectors.toList());
        List<InvoiceLineResponseDto> result = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            for (InvoiceLine line : lines) {
                result.add(toInvoiceLineResponseDto(invoice, line));
            }
        }
        
        return result;
    }

    public String createVnpayPaymentUrl(UUID invoiceId, UUID userId, HttpServletRequest request, UUID unitFilter) {
        if (userId == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn: " + invoiceId));

        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cư dân cho user: " + userId));

        if (!residentId.equals(invoice.getPayerResidentId())) {
            throw new IllegalArgumentException("Bạn không có quyền thanh toán hóa đơn này");
        }

        if (unitFilter != null && !unitFilter.equals(invoice.getPayerUnitId())) {
            throw new IllegalArgumentException("Hóa đơn không thuộc căn hộ đã chọn");
        }

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new IllegalStateException("Hóa đơn đã được thanh toán trước đó");
        }

        BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoiceId).stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Hóa đơn không có số tiền cần thanh toán");
        }

        String clientIp = resolveClientIp(request);

        long orderId = Math.abs(invoiceId.hashCode());
        if (orderId == 0) {
            orderId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        }
        orderIdToInvoiceIdMap.put(orderId, invoiceId);

        String orderInfo = "Thanh toán hóa đơn " + (invoice.getCode() != null ? invoice.getCode() : invoiceId);
        String returnUrl = vnpayProperties.getReturnUrl();

        log.info("💳 [InvoiceService] Creating VNPAY URL for invoice={}, user={}, amount={}, ip={}",
                invoiceId, userId, totalAmount, clientIp);

        return vnpayService.createPaymentUrl(orderId, orderInfo, totalAmount, clientIp, returnUrl);
    }

    public VnpayCallbackResult handleVnpayCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Thiếu dữ liệu callback từ VNPAY");
        }

        boolean signatureValid = vnpayService.validateReturn(new HashMap<>(params));
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String txnRef = params.get("vnp_TxnRef");

        UUID invoiceId = getInvoiceIdFromTxnRef(txnRef);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cho txnRef: " + txnRef));

        invoice.setVnpResponseCode(responseCode);
        invoice.setVnpTransactionRef(txnRef);
        invoice.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        invoice.setVnpBankCode(params.get("vnp_BankCode"));
        invoice.setVnpCardType(params.get("vnp_CardType"));

        boolean alreadyPaid = InvoiceStatus.PAID.equals(invoice.getStatus()) && invoice.getPaidAt() != null;

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            if (!alreadyPaid) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaymentGateway("VNPAY");
                invoice.setPaidAt(parseVnpPayDate(params.get("vnp_PayDate")));
                invoiceRepository.save(invoice);
                notifyPaymentSuccess(invoice, params);
                log.info("✅ [InvoiceService] Invoice {} marked as PAID via VNPAY", invoiceId);
            } else {
                log.info("ℹ️ [InvoiceService] Duplicate VNPAY callback received for already paid invoice {}", invoiceId);
            }
            return new VnpayCallbackResult(invoiceId, true, responseCode, true);
        }

        invoiceRepository.save(invoice);
        log.warn("⚠️ [InvoiceService] VNPAY payment failed for invoice {} - responseCode={}, validSignature={}",
                invoiceId, responseCode, signatureValid);
        return new VnpayCallbackResult(invoiceId, false, responseCode, signatureValid);
    }

    public UUID getInvoiceIdFromTxnRef(String txnRef) {
        if (txnRef == null || !txnRef.contains("_")) {
            throw new IllegalArgumentException("Sai định dạng mã giao dịch: " + txnRef);
        }
        try {
            Long orderId = Long.parseLong(txnRef.split("_")[0]);
            UUID invoiceId = orderIdToInvoiceIdMap.get(orderId);
            if (invoiceId == null) {
                invoiceId = invoiceRepository.findByVnpTransactionRef(txnRef)
                        .map(Invoice::getId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Không tìm thấy hóa đơn tương ứng với orderId: " + orderId));
            }
            log.info("🔍 [InvoiceService] Map orderId {} -> invoice {}", orderId, invoiceId);
            return invoiceId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Không thể phân tích mã giao dịch: " + txnRef, ex);
        }
    }

    public List<InvoiceCategoryResponseDto> getUnpaidInvoicesByCategory(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));

        List<Invoice> invoices = invoiceRepository.findByPayerResidentId(residentId);
        invoices = invoices.stream()
                .filter(invoice -> unitFilter.equals(invoice.getPayerUnitId()))
                .filter(invoice -> cycleFilter == null || cycleFilter.equals(invoice.getCycleId()))
                .collect(Collectors.toList());
        Map<String, List<InvoiceLineResponseDto>> grouped = new HashMap<>();

        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
                continue;
            }

            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            for (InvoiceLine line : lines) {
                InvoiceLineResponseDto dto = toInvoiceLineResponseDto(invoice, line);
                if ("PAID".equalsIgnoreCase(dto.getStatus())) {
                    continue;
                }

                String category = determineCategory(line.getServiceCode());
                grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(dto);
            }
        }

        List<InvoiceCategoryResponseDto> response = new ArrayList<>();
        Set<String> processed = new LinkedHashSet<>();

        for (String category : CATEGORY_ORDER) {
            List<InvoiceLineResponseDto> items = grouped.get(category);
            if (items == null || items.isEmpty()) {
                continue;
            }
            response.add(buildCategoryResponse(category, items));
            processed.add(category);
        }

        grouped.forEach((category, items) -> {
            if (items == null || items.isEmpty() || processed.contains(category)) {
                return;
            }
            response.add(buildCategoryResponse(category, items));
        });
        log.debug("🔍 [InvoiceService] Grouped categories: {}", grouped.keySet());
        log.debug("🔍 [InvoiceService] Returning {} categories", response.size());

        return response;
    }

    public List<InvoiceCategoryResponseDto> getPaidInvoicesByCategory(UUID userId, UUID unitFilter, UUID cycleFilter) {
        if (unitFilter == null) {
            throw new IllegalArgumentException("unitId is required");
        }
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));

        List<Invoice> invoices = invoiceRepository.findByPayerResidentId(residentId);
        log.debug("🔍 [InvoiceService] Found {} invoices for resident {}", invoices.size(), residentId);
        invoices = invoices.stream()
                .filter(invoice -> unitFilter.equals(invoice.getPayerUnitId()))
                .filter(invoice -> cycleFilter == null || cycleFilter.equals(invoice.getCycleId()))
                .collect(Collectors.toList());
        log.debug("🔍 [InvoiceService] After unit/cycle filter {} invoices remain for unit {}", invoices.size(), unitFilter);
        Map<String, List<InvoiceLineResponseDto>> grouped = new HashMap<>();

        for (Invoice invoice : invoices) {
            log.debug("🔍 [InvoiceService] Inspect invoice {} status {}", invoice.getId(), invoice.getStatus());
            if (invoice.getStatus() != InvoiceStatus.PAID) {
                continue;
            }

            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());
            log.debug("🔍 [InvoiceService] Invoice {} has {} lines", invoice.getId(), lines.size());
            for (InvoiceLine line : lines) {
                String category = determineCategory(line.getServiceCode());
                InvoiceLineResponseDto dto = toInvoiceLineResponseDto(invoice, line);
                grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(dto);
                log.debug("🔍 [InvoiceService] Added line {} to category {}", line.getId(), category);
            }
        }

        List<InvoiceCategoryResponseDto> response = new ArrayList<>();
        Set<String> processed = new LinkedHashSet<>();

        for (String category : CATEGORY_ORDER) {
            List<InvoiceLineResponseDto> items = grouped.get(category);
            if (items == null || items.isEmpty()) {
                continue;
            }
            response.add(buildCategoryResponse(category, items));
            processed.add(category);
        }

        grouped.forEach((category, items) -> {
            if (items == null || items.isEmpty() || processed.contains(category)) {
                return;
            }
            response.add(buildCategoryResponse(category, items));
        });

        return response;
    }
    
    public List<ElectricityMonthlyDto> getElectricityMonthlyData(UUID userId, UUID unitFilter) {
        UUID residentId = residentRepository.findResidentIdByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + userId));
        
        List<Invoice> invoices = invoiceRepository.findByPayerResidentId(residentId);
        if (unitFilter != null) {
            invoices = invoices.stream()
                    .filter(invoice -> unitFilter.equals(invoice.getPayerUnitId()))
                    .collect(Collectors.toList());
        }
        List<InvoiceLine> electricityLines = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdAndServiceCode(
                    invoice.getId(), "ELECTRIC");
            electricityLines.addAll(lines);
        }
        
        // Group by month
        Map<String, List<InvoiceLine>> linesByMonth = electricityLines.stream()
                .collect(Collectors.groupingBy(line -> {
                    LocalDate serviceDate = line.getServiceDate();
                    return String.format("%04d-%02d", serviceDate.getYear(), serviceDate.getMonthValue());
                }));
        
        List<ElectricityMonthlyDto> result = new ArrayList<>();
        for (Map.Entry<String, List<InvoiceLine>> entry : linesByMonth.entrySet()) {
            String month = entry.getKey();
            List<InvoiceLine> lines = entry.getValue();
            
            BigDecimal totalAmount = lines.stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            LocalDate firstDate = lines.get(0).getServiceDate();
            String monthDisplay = String.format("%02d/%04d", firstDate.getMonthValue(), firstDate.getYear());
            
            result.add(ElectricityMonthlyDto.builder()
                    .month(month)
                    .monthDisplay(monthDisplay)
                    .amount(totalAmount)
                    .year(firstDate.getYear())
                    .monthNumber(firstDate.getMonthValue())
                    .build());
        }
        
        // Sort by month descending (newest first)
        result.sort((a, b) -> {
            int yearCompare = b.getYear().compareTo(a.getYear());
            if (yearCompare != 0) return yearCompare;
            return b.getMonthNumber().compareTo(a.getMonthNumber());
        });
        
        return result;
    }
    
    public record VnpayCallbackResult(UUID invoiceId, boolean success, String responseCode, boolean signatureValid) {}

    private void notifyPaymentSuccess(Invoice invoice, Map<String, String> params) {
        if (invoice.getPayerResidentId() == null) {
            return;
        }

        Optional<ResidentContact> contactOpt = residentRepository.findContactByResidentId(invoice.getPayerResidentId());
        if (contactOpt.isEmpty() || contactOpt.get().email() == null || contactOpt.get().email().isBlank()) {
            log.warn("⚠️ [InvoiceService] Không tìm thấy email cư dân để gửi thông báo thanh toán");
            return;
        }

        ResidentContact contact = contactOpt.get();
        String email = contact.email();
        String customerName = contact.fullName() != null ? contact.fullName() : email;

        BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);

        String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
        String amountText = currencyFormat.format(totalAmount);
        OffsetDateTime paidAt = Optional.ofNullable(invoice.getPaidAt()).orElse(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        String paidAtText = paidAt.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String txnRef = params.get("vnp_TxnRef");

        String subject = "Thanh toán thành công - Hóa đơn " + invoiceCode;
        String body = "Xin chào " + customerName + ",\n\n" +
                "Thanh toán hóa đơn của bạn đã được xử lý thành công.\n\n" +
                "Thông tin thanh toán:\n" +
                "- Mã hóa đơn: " + invoiceCode + "\n" +
                "- Số tiền: " + amountText + "\n" +
                "- Ngày thanh toán: " + paidAtText + "\n" +
                "- Phương thức: VNPAY\n" +
                (txnRef != null ? "- Mã giao dịch: " + txnRef + "\n" : "") +
                "\nCảm ơn bạn đã sử dụng dịch vụ của QHomeBase!\n\n" +
                "Trân trọng,\n" +
                "QHomeBase";

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Không thể gửi email thông báo thanh toán cho {}: {}", email, e.getMessage());
        }
    }

    private OffsetDateTime parseVnpPayDate(String payDate) {
        if (payDate == null || payDate.isBlank()) {
            return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime localDateTime = LocalDateTime.parse(payDate, formatter);
            return localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        } catch (Exception e) {
            log.warn("⚠️ [InvoiceService] Không thể parse vnp_PayDate {}: {}", payDate, e.getMessage());
            return OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
                .paymentGateway(invoice.getPaymentGateway())
                .vnpTransactionRef(invoice.getVnpTransactionRef())
                .vnpTransactionNo(invoice.getVnpTransactionNo())
                .vnpBankCode(invoice.getVnpBankCode())
                .vnpCardType(invoice.getVnpCardType())
                .vnpResponseCode(invoice.getVnpResponseCode())
                .paidAt(invoice.getPaidAt())
                .lines(lines.stream().map(this::lineToDto).collect(Collectors.toList()))
                .build();
    }

    public InvoiceDto mapToDto(Invoice invoice) {
        return toDto(invoice);
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
    
    private InvoiceLineResponseDto toInvoiceLineResponseDto(Invoice invoice, InvoiceLine line) {
        return InvoiceLineResponseDto.builder()
                .payerUnitId(invoice.getPayerUnitId() != null ? invoice.getPayerUnitId().toString() : "")
                .invoiceId(invoice.getId().toString())
                .serviceDate(line.getServiceDate().toString())
                .description(line.getDescription())
                .quantity(line.getQuantity() != null ? line.getQuantity().doubleValue() : 0.0)
                .unit(line.getUnit())
                .unitPrice(line.getUnitPrice() != null ? line.getUnitPrice().doubleValue() : 0.0)
                .taxAmount(line.getTaxAmount() != null ? line.getTaxAmount().doubleValue() : 0.0)
                .lineTotal(line.getLineTotal() != null ? line.getLineTotal().doubleValue() : 0.0)
                .serviceCode(line.getServiceCode())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : "PUBLISHED")
                .build();
    }

    private InvoiceCategoryResponseDto buildCategoryResponse(String category, List<InvoiceLineResponseDto> invoices) {
        double total = invoices.stream()
                .mapToDouble(item -> item.getLineTotal() != null ? item.getLineTotal() : 0.0)
                .sum();

        return InvoiceCategoryResponseDto.builder()
                .categoryCode(category)
                .categoryName(resolveCategoryName(category))
                .totalAmount(total)
                .invoiceCount(invoices.size())
                .invoices(invoices)
                .build();
    }

    private String determineCategory(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return "OTHER";
        }
        String normalized = serviceCode.trim().toUpperCase();

        if (normalized.contains("ELECTRIC")) {
            return "ELECTRICITY";
        }
        if (normalized.contains("WATER")) {
            return "WATER";
        }
        if (normalized.contains("INTERNET") || normalized.contains("WIFI")) {
            return "INTERNET";
        }
        if (normalized.contains("ELEVATOR")) {
            return "ELEVATOR";
        }
        if (normalized.contains("PARK") || normalized.contains("VEHICLE") || normalized.contains("CAR") || normalized.contains("MOTOR")) {
            return "PARKING";
        }

        return "OTHER";
    }

    private String resolveCategoryName(String categoryCode) {
        return CATEGORY_LABELS.getOrDefault(categoryCode, categoryCode);
    }

    private String buildVehicleRegistrationDescription(VehicleRegistrationPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ xe");
        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            builder.append(" - ").append(request.getLicensePlate());
        }
        if (request.getVehicleType() != null && !request.getVehicleType().isBlank()) {
            builder.append(" (").append(request.getVehicleType()).append(")");
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            builder.append(" - ").append(request.getNote());
        }
        return builder.toString();
    }

    @Transactional
    public InvoiceDto recordElevatorCardPayment(ElevatorCardPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildElevatorCardDescription(request);

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(request.getFullName())
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("ELEVATOR_CARD")
                        .externalRefType("ELEVATOR_CARD")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }

    private String buildElevatorCardDescription(ElevatorCardPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ thang máy");
        if (request.getApartmentNumber() != null && !request.getApartmentNumber().isBlank()) {
            builder.append(" - Căn ").append(request.getApartmentNumber());
        }
        if (request.getBuildingName() != null && !request.getBuildingName().isBlank()) {
            builder.append(" (").append(request.getBuildingName()).append(")");
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            builder.append(" - ").append(request.getFullName());
        }
        return builder.toString();
    }

    @Transactional
    public InvoiceDto recordResidentCardPayment(ResidentCardPaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Missing userId");
        }

        UUID residentId = residentRepository.findResidentIdByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found for user: " + request.getUserId()));

        BigDecimal amount = Optional.ofNullable(request.getAmount())
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.valueOf(30000));

        OffsetDateTime payDate = Optional.ofNullable(request.getPaymentDate())
                .orElse(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        LocalDate serviceDate = payDate.toLocalDate();

        String description = buildResidentCardDescription(request);

        CreateInvoiceRequest createRequest = CreateInvoiceRequest.builder()
                .dueDate(serviceDate)
                .currency("VND")
                .billToName(request.getFullName())
                .payerUnitId(request.getUnitId())
                .payerResidentId(residentId)
                .lines(List.of(CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(description)
                        .quantity(BigDecimal.ONE)
                        .unit("lần")
                        .unitPrice(amount)
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode("RESIDENT_CARD")
                        .externalRefType("RESIDENT_CARD")
                        .externalRefId(request.getRegistrationId())
                        .build()))
                .build();

        InvoiceDto created = createInvoice(createRequest);

        Invoice invoice = invoiceRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + created.getId()));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentGateway("VNPAY");
        invoice.setPaidAt(payDate);
        invoice.setVnpTransactionRef(request.getTransactionRef());
        invoice.setVnpTransactionNo(request.getTransactionNo());
        invoice.setVnpBankCode(request.getBankCode());
        invoice.setVnpCardType(request.getCardType());
        invoice.setVnpResponseCode(request.getResponseCode());
        invoiceRepository.save(invoice);

        Map<String, String> params = new HashMap<>();
        if (request.getTransactionRef() != null) {
            params.put("vnp_TxnRef", request.getTransactionRef());
        }
        notifyPaymentSuccess(invoice, params);

        return toDto(invoice);
    }

    private String buildResidentCardDescription(ResidentCardPaymentRequest request) {
        StringBuilder builder = new StringBuilder("Đăng ký thẻ cư dân");
        if (request.getApartmentNumber() != null && !request.getApartmentNumber().isBlank()) {
            builder.append(" - Căn ").append(request.getApartmentNumber());
        }
        if (request.getBuildingName() != null && !request.getBuildingName().isBlank()) {
            builder.append(" (").append(request.getBuildingName()).append(")");
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            builder.append(" - ").append(request.getFullName());
        }
        return builder.toString();
    }
}

