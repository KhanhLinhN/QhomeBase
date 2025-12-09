package com.QhomeBase.financebillingservice.jobs;

import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically expire VNPay payments that have been pending for more than 10 minutes.
 * This job runs every minute to check for invoices with:
 * - paymentGateway = "VNPAY"
 * - status != "PAID"
 * - vnpayInitiatedAt != null and more than 10 minutes ago
 * 
 * When found, it marks the payment as failed by setting vnpResponseCode to "TIMEOUT".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VnpayPaymentExpiryJob {

    private final InvoiceRepository invoiceRepository;

    @Value("${vnpay.payment.timeout-minutes:10}")
    private int timeoutMinutes;

    @Value("${vnpay.payment.sweep-interval-ms:60000}")
    private long sweepIntervalMs;

    /**
     * Chạy mỗi phút để check và expire các payment VNPay quá thời gian
     * Default: check mỗi 60 giây (60000ms)
     */
    @Scheduled(fixedDelayString = "${vnpay.payment.sweep-interval-ms:60000}")
    @Transactional
    public void expirePendingVnpayPayments() {
        try {
            final OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(timeoutMinutes);
            
            // Tìm các invoice có VNPay payment đang pending quá thời gian timeout
            List<Invoice> expiredPayments = invoiceRepository.findExpiredVnpayPayments(threshold);

            if (expiredPayments.isEmpty()) {
                return;
            }

            log.info("⏰ [VnpayPaymentExpiryJob] Found {} VNPay payment(s) expired (older than {} minutes)", 
                    expiredPayments.size(), timeoutMinutes);

            int expiredCount = 0;
            for (Invoice invoice : expiredPayments) {
                // Chỉ expire nếu chưa được thanh toán
                if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
                    continue;
                }

                // Set response code to indicate timeout
                invoice.setVnpResponseCode("TIMEOUT");
                
                // Log the expiration
                log.info("❌ [VnpayPaymentExpiryJob] Expiring VNPay payment for invoice {} (initiated at: {}, elapsed: {} minutes)", 
                        invoice.getId(), 
                        invoice.getVnpayInitiatedAt(),
                        java.time.Duration.between(invoice.getVnpayInitiatedAt(), OffsetDateTime.now()).toMinutes());

                invoiceRepository.save(invoice);
                expiredCount++;
            }

            if (expiredCount > 0) {
                log.info("✅ [VnpayPaymentExpiryJob] Expired {} VNPay payment(s) after {} minutes timeout", 
                        expiredCount, timeoutMinutes);
            }

        } catch (Exception e) {
            log.error("❌ [VnpayPaymentExpiryJob] Error expiring pending VNPay payments", e);
        }
    }
}
