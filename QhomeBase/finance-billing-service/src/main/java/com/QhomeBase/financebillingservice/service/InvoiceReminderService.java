package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReminderService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int REMINDER_INTERVAL_HOURS = 24;
    private static final int MAX_REMINDERS = 7;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;

    @Value("${invoice.reminder.enabled:true}")
    private boolean remindersEnabled;

    public InvoiceRepository getInvoiceRepository() {
        return invoiceRepository;
    }

    /**
     * Find invoices that need reminder:
     * 1. Status = PUBLISHED (chưa thanh toán)
     * 2. Đã qua 24 giờ từ khi tạo (issued_at + 24h <= now)
     * 3. Chưa nhắc đủ 7 lần (reminder_count < 7)
     * 4. Đã qua 24 giờ từ lần nhắc cuối (last_reminder_at + 24h <= now) hoặc chưa nhắc lần nào
     */
    @Transactional(readOnly = true)
    public List<Invoice> findInvoicesNeedingReminder() {
        if (!remindersEnabled) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now(ZONE);

        // Find invoices that:
        // 1. Status = PUBLISHED
        // 2. Issued at least 24 hours ago
        // 3. Reminder count < 7
        // 4. Last reminder was at least 24 hours ago (or never reminded)
        List<Invoice> allPublished = invoiceRepository.findByStatus(InvoiceStatus.PUBLISHED);
        
        return allPublished.stream()
                .filter(invoice -> {
                    // Check if issued at least 24 hours ago
                    if (invoice.getIssuedAt() == null) {
                        return false;
                    }
                    OffsetDateTime firstReminderTime = invoice.getIssuedAt().plusHours(REMINDER_INTERVAL_HOURS);
                    if (now.isBefore(firstReminderTime)) {
                        return false; // Chưa đến 24 giờ từ khi tạo
                    }

                    // Check reminder count
                    int reminderCount = invoice.getReminderCount() != null ? invoice.getReminderCount() : 0;
                    if (reminderCount >= MAX_REMINDERS) {
                        return false; // Đã nhắc đủ 7 lần
                    }

                    // Check if last reminder was at least 24 hours ago (or never reminded)
                    if (invoice.getLastReminderAt() != null) {
                        OffsetDateTime nextReminderTime = invoice.getLastReminderAt().plusHours(REMINDER_INTERVAL_HOURS);
                        if (now.isBefore(nextReminderTime)) {
                            return false; // Chưa đến 24 giờ từ lần nhắc cuối
                        }
                    }

                    return true;
                })
                .toList();
    }

    /**
     * Send reminder notification for an invoice
     */
    @Transactional
    public void sendReminder(Invoice invoice) {
        if (invoice.getPayerResidentId() == null) {
            log.warn("⚠️ [InvoiceReminderService] Cannot send reminder: payerResidentId is null for invoice {}", invoice.getId());
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
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceReminderService] Failed to get buildingId from unitId {}: {}", 
                            invoice.getPayerUnitId(), e.getMessage());
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
            int reminderCount = invoice.getReminderCount() != null ? invoice.getReminderCount() : 0;
            int remainingReminders = MAX_REMINDERS - reminderCount - 1;
            
            String title = "Nhắc nhở thanh toán hóa đơn - " + invoiceCode;
            String message = String.format(
                    "Bạn có hóa đơn chưa thanh toán với số tiền %s. Hạn thanh toán: %s. Vui lòng thanh toán sớm để tránh gián đoạn dịch vụ.",
                    amountText,
                    invoice.getDueDate() != null 
                            ? invoice.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                            : "N/A");

            // Prepare data payload
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("invoiceCode", invoiceCode);
            data.put("amount", totalAmount.toString());
            data.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
            data.put("reminderCount", String.valueOf(reminderCount + 1));
            data.put("remainingReminders", String.valueOf(remainingReminders));

            // Send notification
            notificationClient.sendResidentNotification(
                    invoice.getPayerResidentId(),
                    buildingId,
                    "BILL",
                    title,
                    message,
                    invoice.getId(),
                    "INVOICE_REMINDER",
                    data
            );

            // Update reminder count and last reminder time
            invoice.setReminderCount(reminderCount + 1);
            invoice.setLastReminderAt(OffsetDateTime.now(ZONE));
            invoiceRepository.save(invoice);

            log.info("✅ [InvoiceReminderService] Sent reminder #{} to residentId={}, buildingId={}, invoiceId={}", 
                    reminderCount + 1, invoice.getPayerResidentId(), buildingId, invoice.getId());
        } catch (Exception e) {
            log.error("❌ [InvoiceReminderService] Failed to send reminder for invoiceId={}: {}", 
                    invoice.getId(), e.getMessage(), e);
        }
    }
}

