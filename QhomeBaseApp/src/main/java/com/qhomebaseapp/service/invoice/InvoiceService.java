package com.qhomebaseapp.service.invoice;

import com.qhomebaseapp.dto.invoice.InvoiceDto;
import com.qhomebaseapp.dto.invoice.InvoiceLineDto;
import com.qhomebaseapp.dto.invoice.InvoiceLineResponseDto;
import com.qhomebaseapp.dto.invoice.UpdateInvoiceStatusRequest;
import com.qhomebaseapp.service.vnpay.VnpayService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final RestTemplate restTemplate;
    private final VnpayService vnpayService;

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
                log.debug("📋 Invoice: id={}, code={}, status={}, lines={}", 
                        invoice.getId(), invoice.getCode(), invoice.getStatus(),
                        invoice.getLines() != null ? invoice.getLines().size() : 0);
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
            // Lấy tất cả invoices từ admin API
            List<InvoiceDto> invoices = getInvoicesByUnitId(unitId);
            
            // Transform: flatten invoice lines thành danh sách items
            List<InvoiceLineResponseDto> result = new ArrayList<>();
            
            for (InvoiceDto invoice : invoices) {
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
                    }
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
    public void handleVnpayCallback(String invoiceId, Map<String, String> vnpParams) {
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
}

