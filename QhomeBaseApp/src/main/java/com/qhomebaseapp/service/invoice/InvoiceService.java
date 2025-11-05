package com.qhomebaseapp.service.invoice;

import com.qhomebaseapp.dto.invoice.InvoiceDto;
import com.qhomebaseapp.dto.invoice.InvoiceLineDto;
import com.qhomebaseapp.dto.invoice.InvoiceLineResponseDto;
import com.qhomebaseapp.dto.invoice.UnifiedPaidInvoiceDto;
import com.qhomebaseapp.dto.invoice.UpdateInvoiceStatusRequest;
import com.qhomebaseapp.dto.invoice.ElectricityMonthlyDto;
import com.qhomebaseapp.dto.service.ServiceBookingResponseDto;
import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.service.service.ServiceBookingService;
import com.qhomebaseapp.service.registerregistration.RegisterRegistrationService;
import com.qhomebaseapp.service.vnpay.VnpayService;
import com.qhomebaseapp.service.user.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final RestTemplate restTemplate;
    private final VnpayService vnpayService;
    private final EmailService emailService;
    private final ServiceBookingService serviceBookingService;
    private final RegisterRegistrationService registerRegistrationService;
    private final UserRepository userRepository;

    @Value("${admin.api.base-url}")
    private String adminApiBaseUrl;

    // Lưu mapping invoiceId -> orderId để có thể reverse lookup khi callback
    private final Map<Long, String> orderIdToInvoiceIdMap = new ConcurrentHashMap<>();

    /**
     * Lấy danh sách hóa đơn theo unitId từ admin API
     */
    public List<InvoiceDto> getInvoicesByUnitId(String unitId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(adminApiBaseUrl)
                    .path("/invoices/unit/{unitId}")
                    .buildAndExpand(unitId)
                    .toUriString();

            log.info("🔍 [InvoiceService] Gọi admin API để lấy invoices cho unitId: {}", unitId);
            log.info("📍 URL: {}", url);

            ResponseEntity<List<InvoiceDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<InvoiceDto>>() {}
            );

            List<InvoiceDto> invoices = response.getBody();
            
            if (invoices == null) {
                log.warn("⚠️ [InvoiceService] Admin API trả về null cho unitId: {}", unitId);
                return List.of();
            }
            
            log.info("✅ [InvoiceService] Lấy được {} invoices cho unitId: {}", invoices.size(), unitId);
            
            // Log chi tiết để debug
            for (InvoiceDto invoice : invoices) {
                log.info("📋 Invoice: id={}, code={}, status={}, payerUnitId={}, lines={}", 
                        invoice.getId(), invoice.getCode(), invoice.getStatus(), 
                        invoice.getPayerUnitId(),
                        invoice.getLines() != null ? invoice.getLines().size() : 0);
                
                if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
                    for (InvoiceLineDto line : invoice.getLines()) {
                        log.info("  └─ Line: description={}, serviceDate={}, lineTotal={}", 
                                line.getDescription(), line.getServiceDate(), line.getLineTotal());
                    }
                }
            }

            return invoices;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ [InvoiceService] HTTP Error khi gọi admin API: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Lỗi khi gọi admin API: " + e.getStatusCode() + " - " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("❌ [InvoiceService] Server Error khi gọi admin API: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Lỗi server admin API: " + e.getStatusCode() + " - " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi gọi admin API để lấy invoices cho unitId: {} - {}", 
                    unitId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách hóa đơn từ hệ thống admin: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật trạng thái hóa đơn thành PAID
     */
    public InvoiceDto updateInvoiceStatus(String invoiceId, String status) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(adminApiBaseUrl)
                    .path("/invoices/{invoiceId}/status")
                    .buildAndExpand(invoiceId)
                    .toUriString();

            UpdateInvoiceStatusRequest request = new UpdateInvoiceStatusRequest(status);

            log.info("💳 [InvoiceService] Cập nhật trạng thái invoice {} thành {}", invoiceId, status);
            log.info("📍 URL: {}", url);

            HttpEntity<UpdateInvoiceStatusRequest> httpEntity = new HttpEntity<>(request);

            ResponseEntity<InvoiceDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    httpEntity,
                    InvoiceDto.class
            );

            InvoiceDto updatedInvoice = response.getBody();
            log.info("✅ [InvoiceService] Đã cập nhật trạng thái invoice {} thành công", invoiceId);

            return updatedInvoice;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi cập nhật trạng thái invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật trạng thái hóa đơn: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy chi tiết một hóa đơn theo invoiceId
     */
    public InvoiceDto getInvoiceById(String invoiceId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(adminApiBaseUrl)
                    .path("/invoices/{invoiceId}")
                    .buildAndExpand(invoiceId)
                    .toUriString();

            log.info("🔍 [InvoiceService] Lấy chi tiết invoice: {}", invoiceId);

            ResponseEntity<InvoiceDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    InvoiceDto.class
            );

            InvoiceDto invoice = response.getBody();
            log.info("✅ [InvoiceService] Lấy được chi tiết invoice {}", invoiceId);

            return invoice;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi lấy chi tiết invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy chi tiết hóa đơn: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách invoice lines theo format Flutter cần
     * Transform từ InvoiceDto sang InvoiceLineResponseDto
     */
    public List<InvoiceLineResponseDto> getInvoiceLinesForFlutter(String unitId) {
        try {
            log.info("🔄 [InvoiceService] Bắt đầu getInvoiceLinesForFlutter với unitId: {}", unitId);
            
            // Lấy tất cả invoices từ admin API
            List<InvoiceDto> invoices = getInvoicesByUnitId(unitId);
            
            log.info("📊 [InvoiceService] Số lượng invoices nhận được từ admin API: {}", invoices.size());
            
            // Transform: flatten invoice lines thành danh sách items
            List<InvoiceLineResponseDto> result = new ArrayList<>();
            
            for (InvoiceDto invoice : invoices) {
                log.info("🔍 [InvoiceService] Xử lý invoice: id={}, code={}, payerUnitId={}, lines={}", 
                        invoice.getId(), invoice.getCode(), invoice.getPayerUnitId(),
                        invoice.getLines() != null ? invoice.getLines().size() : 0);
                
                if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
                    for (InvoiceLineDto line : invoice.getLines()) {
                        InvoiceLineResponseDto responseDto = InvoiceLineResponseDto.builder()
                                .payerUnitId(invoice.getPayerUnitId())
                                .invoiceId(invoice.getId()) // Lấy ID từ invoice, không phải từ line
                                .serviceDate(line.getServiceDate())
                                .description(line.getDescription())
                                .quantity(line.getQuantity())
                                .unit(line.getUnit())
                                .unitPrice(line.getUnitPrice())
                                .taxAmount(line.getTaxAmount())
                                .lineTotal(line.getLineTotal())
                                .serviceCode(line.getServiceCode())
                                .status(invoice.getStatus())
                                .build();
                        
                        result.add(responseDto);
                        log.debug("  ✅ Đã thêm line: description={}, lineTotal={}", 
                                line.getDescription(), line.getLineTotal());
                    }
                } else {
                    log.warn("  ⚠️ Invoice {} không có lines hoặc lines rỗng", invoice.getId());
                }
            }
            
            log.info("✅ [InvoiceService] Transform thành công: {} invoice lines cho unitId: {}", 
                    result.size(), unitId);
            
            return result;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi transform invoice lines cho unitId: {}", unitId, e);
            throw new RuntimeException("Không thể lấy danh sách hóa đơn: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo VNPAY payment URL cho invoice
     */
    public String createVnpayPaymentUrl(String invoiceId, HttpServletRequest request) {
        try {
            // Lấy chi tiết invoice để có totalAmount
            InvoiceDto invoice = getInvoiceById(invoiceId);
            
            if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
                throw new RuntimeException("Hóa đơn đã được thanh toán trước đó");
            }
            
            BigDecimal amount = invoice.getTotalAmount() != null 
                    ? invoice.getTotalAmount() 
                    : BigDecimal.ZERO;
            
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getRemoteAddr();
            }
            
            // Tạo orderId từ invoiceId (hashCode để có số nguyên)
            // Lưu mapping để có thể reverse lookup khi callback
            Long orderId = Math.abs((long) invoiceId.hashCode());
            
            // Lưu mapping orderId -> invoiceId
            orderIdToInvoiceIdMap.put(orderId, invoiceId);
            
            String orderInfo = "Thanh toán hóa đơn " + invoice.getCode();
            
            String paymentUrl = vnpayService.createPaymentUrl(orderId, orderInfo, amount, clientIp);
            
            log.info("💳 [InvoiceService] Tạo VNPAY URL cho invoice: {}, orderId: {}", invoiceId, orderId);
            
            return paymentUrl;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi tạo VNPAY URL cho invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo URL thanh toán VNPAY: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý VNPAY callback và cập nhật invoice status thành PAID
     */
    public void handleVnpayCallback(String invoiceId, Map<String, String> vnpParams, String userEmail) {
        try {
            // Validate VNPAY response
            boolean valid = vnpayService.validateReturn(new HashMap<>(vnpParams));
            
            String responseCode = vnpParams.get("vnp_ResponseCode");
            String transactionStatus = vnpParams.get("vnp_TransactionStatus");
            
            log.info("💳 [InvoiceService] VNPAY callback cho invoice: {}, valid: {}, responseCode: {}", 
                    invoiceId, valid, responseCode);
            
            if (valid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
                // Cập nhật status thành PAID
                updateInvoiceStatus(invoiceId, "PAID");
                log.info("✅ [InvoiceService] Đã cập nhật invoice {} sang PAID sau khi thanh toán VNPAY", invoiceId);
                
                // Gửi email thông báo thanh toán thành công
                try {
                    if (userEmail != null && !userEmail.isBlank()) {
                        // Lấy thông tin invoice để tính tổng tiền
                        InvoiceDto invoice = getInvoiceById(invoiceId);
                        BigDecimal totalAmount = BigDecimal.ZERO;
                        if (invoice.getLines() != null) {
                            for (InvoiceLineDto line : invoice.getLines()) {
                                if (line.getLineTotal() != null) {
                                    totalAmount = totalAmount.add(line.getLineTotal());
                                }
                            }
                        }
                        
                        String emailSubject = "Thanh toán thành công - Hóa đơn #" + invoice.getCode();
                        LocalDateTime paymentDateTime = LocalDateTime.now();
                        String paymentDateStr = paymentDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                        String amountStr = currencyFormat.format(totalAmount) + " VNĐ";
                        String paymentMethod = "VNPAY";
                        String txnRef = vnpParams.get("vnp_TxnRef");
                        
                        String emailBody = String.format(
                            "Xin chào %s,\n\n" +
                            "Thanh toán hóa đơn của bạn đã được xử lý thành công!\n\n" +
                            "Thông tin thanh toán:\n" +
                            "- Mã hóa đơn: %s\n" +
                            "- Tổng số tiền: %s\n" +
                            "- Ngày giờ thanh toán: %s\n" +
                            "- Phương thức thanh toán: %s\n" +
                            "%s\n\n" +
                            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!\n\n" +
                            "Trân trọng,\n" +
                            "Hệ thống QHomeBase",
                            userEmail.split("@")[0], // Tên user từ email
                            invoice.getCode() != null ? invoice.getCode() : invoiceId,
                            amountStr,
                            paymentDateStr,
                            paymentMethod,
                            txnRef != null ? "- Mã giao dịch: " + txnRef : ""
                        );
                        
                        emailService.sendEmail(userEmail, emailSubject, emailBody);
                        log.info("✅ [InvoiceService] Đã gửi email thông báo thanh toán thành công cho user: {}", userEmail);
                    }
                } catch (Exception e) {
                    log.error("❌ [InvoiceService] Lỗi khi gửi email thông báo thanh toán: {}", e.getMessage(), e);
                    // Không throw exception để không ảnh hưởng đến flow thanh toán
                }
            } else {
                throw new RuntimeException("Thanh toán thất bại hoặc chữ ký không hợp lệ");
            }
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi xử lý VNPAY callback cho invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("Lỗi xử lý kết quả thanh toán VNPAY: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy invoiceId từ txnRef (vnpay transaction reference)
     * Format: orderId_timestamp, trong đó orderId được lưu trong mapping
     */
    public String getInvoiceIdFromTxnRef(String txnRef) {
        if (txnRef == null || !txnRef.contains("_")) {
            throw new RuntimeException("Thiếu hoặc sai định dạng vnp_TxnRef: " + txnRef);
        }
        
        try {
            Long orderId = Long.parseLong(txnRef.split("_")[0]);
            String invoiceId = orderIdToInvoiceIdMap.get(orderId);
            
            if (invoiceId == null) {
                throw new RuntimeException("Không tìm thấy invoiceId cho orderId: " + orderId);
            }
            
            log.info("🔍 [InvoiceService] Map orderId {} -> invoiceId {}", orderId, invoiceId);
            return invoiceId;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi parse txnRef {}: {}", txnRef, e.getMessage());
            throw new RuntimeException("Không thể lấy invoiceId từ txnRef: " + txnRef, e);
        }
    }

    /**
     * Thanh toán hóa đơn - cập nhật status thành PAID (không dùng VNPAY)
     */
    public void payInvoice(String invoiceId) {
        updateInvoiceStatus(invoiceId, "PAID");
        log.info("✅ [InvoiceService] Đã thanh toán invoice: {}", invoiceId);
    }

    /**
     * Lấy tất cả các hóa đơn đã thanh toán từ tất cả các nguồn:
     * - Hóa đơn điện (invoices từ admin API)
     * - Hóa đơn dịch vụ (service bookings)
     * - Hóa đơn đăng ký thẻ xe (vehicle registrations)
     */
    public List<UnifiedPaidInvoiceDto> getAllPaidInvoices(Long userId) {
        List<UnifiedPaidInvoiceDto> result = new ArrayList<>();
        
        try {
            // 1. Lấy paid invoices từ admin API (hóa đơn điện)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getUnitId() != null && !user.getUnitId().isBlank()) {
                List<InvoiceDto> invoices = getInvoicesByUnitId(user.getUnitId());
                for (InvoiceDto invoice : invoices) {
                    if ("PAID".equalsIgnoreCase(invoice.getStatus()) && invoice.getTotalAmount() != null) {
                        UnifiedPaidInvoiceDto dto = UnifiedPaidInvoiceDto.builder()
                                .id(invoice.getId())
                                .category("ELECTRICITY")
                                .categoryName("Hóa đơn điện")
                                .title(invoice.getCode() != null ? invoice.getCode() : invoice.getId())
                                .description(invoice.getLines() != null && !invoice.getLines().isEmpty() 
                                        ? invoice.getLines().get(0).getDescription() 
                                        : "Hóa đơn điện")
                                .amount(invoice.getTotalAmount())
                                .paymentDate(invoice.getIssuedAt() != null 
                                        ? invoice.getIssuedAt().atOffset(java.time.ZoneOffset.UTC)
                                        : OffsetDateTime.now())
                                .paymentGateway("VNPAY")
                                .status(invoice.getStatus())
                                .reference(invoice.getCode())
                                .invoiceCode(invoice.getCode())
                                .build();
                        result.add(dto);
                    }
                }
            }
            
            // 2. Lấy paid service bookings
            List<ServiceBookingResponseDto> bookings = serviceBookingService.getUserBookings(userId);
            for (ServiceBookingResponseDto booking : bookings) {
                if ("PAID".equalsIgnoreCase(booking.getPaymentStatus()) 
                        && booking.getPaymentDate() != null
                        && booking.getTotalAmount() != null) {
                    UnifiedPaidInvoiceDto dto = UnifiedPaidInvoiceDto.builder()
                            .id(booking.getId().toString())
                            .category("SERVICE_BOOKING")
                            .categoryName("Hóa đơn dịch vụ")
                            .title(booking.getServiceName() != null ? booking.getServiceName() : "Dịch vụ")
                            .description(String.format("%s - %s", 
                                    booking.getBookingDate() != null ? booking.getBookingDate().toString() : "",
                                    booking.getPurpose() != null ? booking.getPurpose() : ""))
                            .amount(booking.getTotalAmount())
                            .paymentDate(booking.getPaymentDate())
                            .paymentGateway(booking.getPaymentGateway())
                            .status(booking.getStatus())
                            .reference(booking.getVnpayTransactionRef())
                            .serviceName(booking.getServiceName())
                            .build();
                    result.add(dto);
                }
            }
            
            // 3. Lấy paid registrations (vehicle và resident card)
            List<RegisterServiceRequestResponseDto> registrations = registerRegistrationService.getByUserId(userId);
            for (RegisterServiceRequestResponseDto registration : registrations) {
                if ("PAID".equalsIgnoreCase(registration.getPaymentStatus()) 
                        && registration.getPaymentDate() != null) {
                    UnifiedPaidInvoiceDto dto;
                    
                    // Check service type
                    if ("RESIDENT_CARD".equalsIgnoreCase(registration.getServiceType())) {
                        // Resident Card registration
                        String title = registration.getResidentName() != null 
                                ? "Đăng ký thẻ cư dân - " + registration.getResidentName()
                                : "Đăng ký thẻ cư dân #" + registration.getId();
                        
                        String description = "";
                        if (registration.getApartmentNumber() != null && registration.getBuildingName() != null) {
                            description = registration.getApartmentNumber() + ", " + registration.getBuildingName();
                        }
                        if (registration.getCitizenId() != null) {
                            if (!description.isEmpty()) description += " - ";
                            description += "CCCD: " + registration.getCitizenId();
                        }
                        if (description.isEmpty()) {
                            description = "Đăng ký thẻ cư dân";
                        }
                        
                        dto = UnifiedPaidInvoiceDto.builder()
                                .id(registration.getId().toString())
                                .category("RESIDENT_CARD_REGISTRATION")
                                .categoryName("Hóa đơn đăng ký thẻ ra vào")
                                .title(title)
                                .description(description)
                                .amount(BigDecimal.valueOf(30000)) // Fixed fee
                                .paymentDate(registration.getPaymentDate())
                                .paymentGateway(registration.getPaymentGateway())
                                .status(registration.getStatus())
                                .reference(registration.getVnpayTransactionRef())
                                .build();
                    } else {
                        // Vehicle registration
                        String title = registration.getLicensePlate() != null 
                                ? "Đăng ký thẻ xe - " + registration.getLicensePlate()
                                : "Đăng ký thẻ xe #" + registration.getId();
                        
                        dto = UnifiedPaidInvoiceDto.builder()
                                .id(registration.getId().toString())
                                .category("VEHICLE_REGISTRATION")
                                .categoryName("Hóa đơn đăng ký thẻ xe")
                                .title(title)
                                .description(registration.getVehicleType() != null 
                                        ? registration.getVehicleType() 
                                        : "Đăng ký thẻ xe")
                                .amount(BigDecimal.valueOf(30000)) // Fixed fee
                                .paymentDate(registration.getPaymentDate())
                                .paymentGateway(registration.getPaymentGateway())
                                .status(registration.getStatus())
                                .reference(registration.getVnpayTransactionRef())
                                .licensePlate(registration.getLicensePlate())
                                .vehicleType(registration.getVehicleType())
                                .build();
                    }
                    
                    result.add(dto);
                }
            }
            
            // Sort by payment date descending (newest first)
            result.sort((a, b) -> {
                if (a.getPaymentDate() == null) return 1;
                if (b.getPaymentDate() == null) return -1;
                return b.getPaymentDate().compareTo(a.getPaymentDate());
            });
            
            log.info("✅ [InvoiceService] Lấy được {} hóa đơn đã thanh toán cho userId: {}", result.size(), userId);
            return result;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi lấy tất cả hóa đơn đã thanh toán cho userId: {}", userId, e);
            throw new RuntimeException("Không thể lấy danh sách hóa đơn đã thanh toán: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy dữ liệu tiền điện theo tháng (12 tháng gần nhất)
     * Filter các invoice lines có serviceCode hoặc description chứa "điện" hoặc "ELECTRICITY"
     */
    public List<ElectricityMonthlyDto> getElectricityMonthlyData(String unitId) {
        try {
            log.info("📊 [InvoiceService] Lấy dữ liệu tiền điện theo tháng cho unitId: {}", unitId);
            
            // Lấy tất cả invoices từ admin API
            List<InvoiceDto> invoices = getInvoicesByUnitId(unitId);
            
            // Filter và group by month
            Map<String, BigDecimal> monthlyAmounts = new HashMap<>();
            
            for (InvoiceDto invoice : invoices) {
                // Chỉ lấy invoices đã thanh toán (PAID)
                if (!"PAID".equalsIgnoreCase(invoice.getStatus())) {
                    continue;
                }
                
                if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
                    for (InvoiceLineDto line : invoice.getLines()) {
                        // Filter lines liên quan đến điện
                        boolean isElectricity = false;
                        if (line.getServiceCode() != null) {
                            String serviceCode = line.getServiceCode().toUpperCase();
                            if (serviceCode.contains("ELECTRICITY") || 
                                serviceCode.contains("ĐIỆN") ||
                                serviceCode.contains("ELEC")) {
                                isElectricity = true;
                            }
                        }
                        if (!isElectricity && line.getDescription() != null) {
                            String description = line.getDescription().toLowerCase();
                            if (description.contains("điện") || 
                                description.contains("electricity") ||
                                description.contains("tiền điện")) {
                                isElectricity = true;
                            }
                        }
                        
                        if (isElectricity && line.getLineTotal() != null && line.getServiceDate() != null) {
                            // Parse serviceDate to get month
                            try {
                                LocalDate serviceDate = LocalDate.parse(line.getServiceDate());
                                String monthKey = YearMonth.from(serviceDate).toString(); // "YYYY-MM"
                                
                                monthlyAmounts.merge(
                                    monthKey,
                                    line.getLineTotal(),
                                    BigDecimal::add
                                );
                            } catch (Exception e) {
                                log.warn("⚠️ [InvoiceService] Không thể parse serviceDate: {}", line.getServiceDate());
                            }
                        }
                    }
                }
            }
            
            // Convert to DTO list and sort by month
            List<ElectricityMonthlyDto> result = monthlyAmounts.entrySet().stream()
                    .map(entry -> {
                        String monthKey = entry.getKey();
                        String[] parts = monthKey.split("-");
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        LocalDate date = LocalDate.of(year, month, 1);
                        
                        return ElectricityMonthlyDto.builder()
                                .month(monthKey)
                                .monthDisplay(DateTimeFormatter.ofPattern("MM/yyyy").format(date))
                                .amount(entry.getValue())
                                .year(year)
                                .monthNumber(month)
                                .build();
                    })
                    .sorted((a, b) -> {
                        // Sort by year first, then month
                        int yearCompare = a.getYear().compareTo(b.getYear());
                        if (yearCompare != 0) return yearCompare;
                        return a.getMonthNumber().compareTo(b.getMonthNumber());
                    })
                    .collect(Collectors.toList());
            
            log.info("✅ [InvoiceService] Lấy được {} tháng có dữ liệu tiền điện", result.size());
            return result;
        } catch (Exception e) {
            log.error("❌ [InvoiceService] Lỗi khi lấy dữ liệu tiền điện theo tháng cho unitId: {}", unitId, e);
            throw new RuntimeException("Không thể lấy dữ liệu tiền điện: " + e.getMessage(), e);
        }
    }
}

