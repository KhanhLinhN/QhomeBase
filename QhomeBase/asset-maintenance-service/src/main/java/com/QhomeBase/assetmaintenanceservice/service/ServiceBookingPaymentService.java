package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.config.VnpayProperties;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingPaymentResponse;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceBookingPaymentResult;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceBookingRepository;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import com.QhomeBase.assetmaintenanceservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBookingPaymentService {

    private static final String PAYMENT_GATEWAY = "VNPAY";

    private final ServiceBookingRepository bookingRepository;
    private final VnpayService vnpayService;
    private final NotificationEmailService emailService;
    private final VnpayProperties vnpayProperties;

    private final ConcurrentMap<Long, UUID> orderIdToBookingId = new ConcurrentHashMap<>();

    @Transactional
    public ServiceBookingPaymentResponse initiatePayment(UUID bookingId,
                                                         Object authenticationPrincipal,
                                                         HttpServletRequest request) {
        ServiceBooking booking = loadBookingForPayment(bookingId, authenticationPrincipal);
        ensurePayable(booking);

        Long orderId = generateOrderId(booking.getId());
        String clientIp = resolveClientIp(request);
        BigDecimal amount = Optional.ofNullable(booking.getTotalAmount())
                .filter(total -> total.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalStateException("Tổng tiền đặt dịch vụ không hợp lệ"));

        String orderInfo = "SERVICE_BOOKING_" + booking.getService().getCode();
        String returnUrl = StringUtils.hasText(vnpayProperties.getServiceBookingReturnUrl())
                ? vnpayProperties.getServiceBookingReturnUrl()
                : vnpayProperties.getReturnUrl();

        String paymentUrl = vnpayService.createPaymentUrl(orderId, orderInfo, amount, clientIp, returnUrl);
        String txnRef = extractTxnRef(paymentUrl);

        booking.setPaymentStatus(ServicePaymentStatus.PENDING);
        booking.setPaymentGateway(PAYMENT_GATEWAY);
        booking.setVnpayTransactionRef(txnRef);

        orderIdToBookingId.put(orderId, booking.getId());
        log.info("💳 [ServiceBooking] Initiated VNPAY payment: bookingId={}, orderId={}, txnRef={}", booking.getId(), orderId, txnRef);

        return new ServiceBookingPaymentResponse(booking.getId(), paymentUrl, txnRef);
    }

    @Transactional
    public ServiceBookingPaymentResult handleCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Thiếu tham số phản hồi từ VNPAY");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (!StringUtils.hasText(txnRef)) {
            throw new IllegalArgumentException("Thiếu mã giao dịch VNPAY");
        }
        UUID mappedBookingId = resolveBookingId(txnRef);
        if (mappedBookingId == null) {
            log.debug("Không tìm thấy mapping tạm thời cho txnRef {}", txnRef);
        }

        ServiceBooking booking = bookingRepository.findByVnpayTransactionRef(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt dịch vụ cho giao dịch: " + txnRef));

        boolean signatureValid = vnpayService.validateReturn(params);
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (signatureValid && "00".equals(responseCode) && "00".equals(transactionStatus)) {
            booking.setPaymentStatus(ServicePaymentStatus.PAID);
            booking.setPaymentGateway(PAYMENT_GATEWAY);
            // Use current time for payment date to ensure accurate timestamp
            booking.setPaymentDate(OffsetDateTime.now());
            bookingRepository.save(booking);

            // Load lazy associations for email notification
            // Force initialization of service (LAZY)
            if (booking.getService() != null) {
                booking.getService().getName();
            }
            // Force initialization of booking items (LAZY)
            if (booking.getBookingItems() != null) {
                booking.getBookingItems().size();
            }

            emailService.sendBookingPaymentSuccess(booking, txnRef);

            removeOrderMapping(txnRef);
            log.info("✅ [ServiceBooking] Thanh toán VNPAY thành công: bookingId={}, txnRef={}", booking.getId(), txnRef);
            return new ServiceBookingPaymentResult(booking.getId(), true, responseCode, true);
        }

        booking.setPaymentStatus(ServicePaymentStatus.UNPAID);
        booking.setPaymentGateway(null);
        bookingRepository.save(booking);

        removeOrderMapping(txnRef);
        log.warn("⚠️ [ServiceBooking] Thanh toán VNPAY thất bại: bookingId={}, txnRef={}, responseCode={}, status={}, signatureValid={}",
                booking.getId(), txnRef, responseCode, transactionStatus, signatureValid);

        return new ServiceBookingPaymentResult(booking.getId(), false, responseCode, signatureValid);
    }

    private void removeOrderMapping(String txnRef) {
        if (!StringUtils.hasText(txnRef)) {
            return;
        }
        String[] parts = txnRef.split("_");
        if (parts.length == 0) {
            return;
        }
        try {
            Long orderId = Long.parseLong(parts[0]);
            orderIdToBookingId.remove(orderId);
        } catch (NumberFormatException ignored) {
            log.debug("🔄 [ServiceBooking] Không thể phân tích orderId từ txnRef {}", txnRef);
        }
    }

    private UUID resolveBookingId(String txnRef) {
        String[] parts = txnRef.split("_");
        if (parts.length == 0) {
            return null;
        }
        try {
            Long orderId = Long.parseLong(parts[0]);
            return orderIdToBookingId.get(orderId);
        } catch (NumberFormatException e) {
            log.warn("Không thể phân tích orderId từ txnRef {}", txnRef);
            return null;
        }
    }


    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    private String extractTxnRef(String paymentUrl) {
        String safePaymentUrl = Objects.requireNonNull(paymentUrl, "paymentUrl");
        if (!StringUtils.hasText(safePaymentUrl)) {
            throw new IllegalArgumentException("URL thanh toán không hợp lệ");
        }
        var uri = UriComponentsBuilder.fromUriString(safePaymentUrl).build();
        var params = uri.getQueryParams();
        var values = params.get("vnp_TxnRef");
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return Objects.requireNonNull(value);
                }
            }
        }
        throw new IllegalStateException("Không thể xác định mã giao dịch VNPAY");
    }

    private void ensurePayable(ServiceBooking booking) {
        if (booking.getStatus() == ServiceBookingStatus.CANCELLED) {
            throw new IllegalStateException("Không thể thanh toán dịch vụ đã bị hủy");
        }
        if (booking.getPaymentStatus() == ServicePaymentStatus.PAID) {
            throw new IllegalStateException("Đơn đặt dịch vụ đã được thanh toán");
        }
    }

    private ServiceBooking loadBookingForPayment(UUID bookingId, Object authenticationPrincipal) {
        if (!(authenticationPrincipal instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Unsupported authentication principal");
        }
        return bookingRepository.findByIdAndUserId(bookingId, principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt dịch vụ"));
    }

    private Long generateOrderId(UUID bookingId) {
        long hash = bookingId.getMostSignificantBits() ^ bookingId.getLeastSignificantBits();
        long timestamp = System.currentTimeMillis();
        return Math.abs(hash + timestamp);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

